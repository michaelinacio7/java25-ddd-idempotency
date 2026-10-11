package com.michaelinacio.idempotency;

import com.michaelinacio.idempotency.application.payment.TransferCommand;
import com.michaelinacio.idempotency.application.payment.TransferService;
import com.michaelinacio.idempotency.domain.payment.BankTransfer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.sql.init.mode=never"
})
class TransferPostgresIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("bankpayments")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired TransferService transferService;

    private UUID customerId;
    private UUID sourceId;
    private UUID destinationOneId;
    private UUID destinationTwoId;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM outbox_events");
        jdbc.update("DELETE FROM ledger_entries");
        jdbc.update("DELETE FROM ledger_transactions");
        jdbc.update("DELETE FROM idempotency_records");
        jdbc.update("DELETE FROM payments");
        jdbc.update("DELETE FROM accounts");
        jdbc.update("DELETE FROM customers");

        customerId = UUID.randomUUID();
        UUID otherCustomerId = UUID.randomUUID();
        sourceId = UUID.randomUUID();
        destinationOneId = UUID.randomUUID();
        destinationTwoId = UUID.randomUUID();

        jdbc.update("INSERT INTO customers(id, external_subject) VALUES (?, ?)",
                customerId, "test-subject-" + customerId);
        jdbc.update("INSERT INTO customers(id, external_subject) VALUES (?, ?)",
                otherCustomerId, "test-subject-" + otherCustomerId);

        insertAccount(sourceId, customerId, "100.00");
        insertAccount(destinationOneId, otherCustomerId, "0.00");
        insertAccount(destinationTwoId, otherCustomerId, "0.00");
    }

    @Test
    void concurrentTransfersCannotOverdrawSourceAccount() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> first = () -> attemptTransfer(destinationOneId, "concurrent-key-1");
            Callable<Boolean> second = () -> attemptTransfer(destinationTwoId, "concurrent-key-2");

            var results = executor.invokeAll(java.util.List.of(first, second));
            long successes = results.stream().filter(future -> {
                try { return future.get(); }
                catch (Exception e) { return false; }
            }).count();

            assertEquals(1, successes, "only one transfer of 80.00 should fit in a 100.00 balance");
            assertEquals(0, new BigDecimal("20.00").compareTo(balanceOf(sourceId)));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM payments", Integer.class));
            assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM ledger_entries", Integer.class));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void sameIdempotencyKeyAndPayloadReturnsSameTransfer() {
        TransferCommand command = new TransferCommand(customerId, sourceId, destinationOneId,
                new BigDecimal("25.00"), "BRL", "same-transfer-key");

        BankTransfer first = transferService.transfer(command);
        BankTransfer replay = transferService.transfer(command);

        assertEquals(first.id(), replay.id());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM payments", Integer.class));
        assertEquals(0, new BigDecimal("75.00").compareTo(balanceOf(sourceId)));
    }

    @Test
    void sameIdempotencyKeyWithDifferentPayloadIsRejected() {
        transferService.transfer(new TransferCommand(customerId, sourceId, destinationOneId,
                new BigDecimal("10.00"), "BRL", "payload-conflict-key"));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> transferService.transfer(new TransferCommand(customerId, sourceId, destinationOneId,
                        new BigDecimal("11.00"), "BRL", "payload-conflict-key")));

        assertEquals("IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_REQUEST", exception.getMessage());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM payments", Integer.class));
    }

    private boolean attemptTransfer(UUID destinationId, String key) {
        try {
            transferService.transfer(new TransferCommand(customerId, sourceId, destinationId,
                    new BigDecimal("80.00"), "BRL", key));
            return true;
        } catch (IllegalStateException expectedForInsufficientBalance) {
            return false;
        }
    }

    private void insertAccount(UUID accountId, UUID ownerId, String balance) {
        jdbc.update("""
                INSERT INTO accounts(id, customer_id, currency, balance, status)
                VALUES (?, ?, 'BRL', ?, 'ACTIVE')
                """, accountId, ownerId, new BigDecimal(balance));
    }

    private BigDecimal balanceOf(UUID accountId) {
        return jdbc.queryForObject("SELECT balance FROM accounts WHERE id = ?",
                BigDecimal.class, accountId);
    }
}
