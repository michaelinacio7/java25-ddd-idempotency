package com.michaelinacio.idempotency.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.michaelinacio.idempotency.domain.account.Money;
import com.michaelinacio.idempotency.domain.payment.BankTransfer;
import com.michaelinacio.idempotency.domain.payment.TransferRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Currency;
import java.util.Map;
import java.util.UUID;

/**
 * PostgreSQL adapter that commits the transfer, both ledger entries, the
 * idempotency response and the outbox event as one database transaction.
 */
@Repository
public class JdbcTransferRepository implements TransferRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public JdbcTransferRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public BankTransfer transfer(UUID customerId, UUID sourceAccountId, UUID destinationAccountId,
                                 Money amount, String idempotencyKey, String requestFingerprint) {
        requireValidRequest(customerId, sourceAccountId, destinationAccountId, amount,
                idempotencyKey, requestFingerprint);

        // Reserve the key first. ON CONFLICT waits for a concurrent transaction
        // and lets us replay its committed result without a check-then-act race.
        int reserved = jdbc.update("""
                INSERT INTO idempotency_records
                    (customer_id, idempotency_key, request_fingerprint)
                VALUES (?, ?, ?)
                ON CONFLICT (customer_id, idempotency_key) DO NOTHING
                """, customerId, idempotencyKey, requestFingerprint);

        if (reserved == 0) {
            return replayExisting(customerId, idempotencyKey, requestFingerprint);
        }

        Map<UUID, AccountRow> accounts = lockAccounts(sourceAccountId, destinationAccountId);
        AccountRow source = accounts.get(sourceAccountId);
        AccountRow destination = accounts.get(destinationAccountId);

        if (source == null || destination == null) {
            throw new IllegalArgumentException("ACCOUNT_NOT_FOUND");
        }
        if (!source.customerId().equals(customerId)) {
            throw new IllegalArgumentException("SOURCE_ACCOUNT_NOT_OWNED_BY_CUSTOMER");
        }
        if (!"ACTIVE".equals(source.status()) || !"ACTIVE".equals(destination.status())) {
            throw new IllegalStateException("ACCOUNT_NOT_ACTIVE");
        }
        if (!source.currency().equals(destination.currency())
                || !source.currency().equals(amount.currency().getCurrencyCode())) {
            throw new IllegalArgumentException("CURRENCY_MISMATCH");
        }
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("INVALID_AMOUNT");
        }
        if (source.balance().compareTo(amount.amount()) < 0) {
            throw new IllegalStateException("INSUFFICIENT_FUNDS");
        }

        UUID transferId = UUID.randomUUID();
        UUID ledgerId = UUID.randomUUID();
        Instant now = Instant.now();
        BigDecimal newSourceBalance = source.balance().subtract(amount.amount());
        BigDecimal newDestinationBalance = destination.balance().add(amount.amount());

        jdbc.update("""
                UPDATE accounts
                   SET balance = ?, version = version + 1
                 WHERE id = ?
                """, newSourceBalance, sourceAccountId);
        jdbc.update("""
                UPDATE accounts
                   SET balance = ?, version = version + 1
                 WHERE id = ?
                """, newDestinationBalance, destinationAccountId);

        jdbc.update("""
                INSERT INTO payments
                    (id, customer_id, source_account_id, destination_account_id,
                     amount, currency, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?, 'POSTED', ?)
                """, transferId, customerId, sourceAccountId, destinationAccountId,
                amount.amount(), amount.currency().getCurrencyCode(), java.sql.Timestamp.from(now));

        jdbc.update("""
                INSERT INTO ledger_transactions (id, payment_id, currency, created_at)
                VALUES (?, ?, ?, ?)
                """, ledgerId, transferId, amount.currency().getCurrencyCode(),
                java.sql.Timestamp.from(now));

        jdbc.update("""
                INSERT INTO ledger_entries
                    (id, ledger_transaction_id, account_id, entry_type, amount, currency, created_at)
                VALUES (?, ?, ?, 'DEBIT', ?, ?, ?),
                       (?, ?, ?, 'CREDIT', ?, ?, ?)
                """,
                UUID.randomUUID(), ledgerId, sourceAccountId, amount.amount(),
                amount.currency().getCurrencyCode(), java.sql.Timestamp.from(now),
                UUID.randomUUID(), ledgerId, destinationAccountId, amount.amount(),
                amount.currency().getCurrencyCode(), java.sql.Timestamp.from(now));

        String responseBody = serialize(Map.of(
                "id", transferId.toString(),
                "sourceAccountId", sourceAccountId.toString(),
                "destinationAccountId", destinationAccountId.toString(),
                "amount", amount.amount().toPlainString(),
                "currency", amount.currency().getCurrencyCode(),
                "status", "POSTED",
                "createdAt", now.toString()));

        jdbc.update("""
                UPDATE idempotency_records
                   SET payment_id = ?, response_status = 201, response_body = ?, completed_at = CURRENT_TIMESTAMP
                 WHERE customer_id = ? AND idempotency_key = ?
                """, transferId, responseBody, customerId, idempotencyKey);

        String eventPayload = serialize(Map.of(
                "transferId", transferId.toString(),
                "sourceAccountId", sourceAccountId.toString(),
                "destinationAccountId", destinationAccountId.toString(),
                "amount", amount.amount().toPlainString(),
                "currency", amount.currency().getCurrencyCode()));

        jdbc.update("""
                INSERT INTO outbox_events (id, aggregate_type, aggregate_id, event_type, payload)
                VALUES (?, 'BankTransfer', ?, 'BankTransferPosted', ?::jsonb)
                """, UUID.randomUUID(), transferId, eventPayload);

        return new BankTransfer(transferId, sourceAccountId, destinationAccountId,
                amount, BankTransfer.Status.POSTED, now);
    }

    private BankTransfer replayExisting(UUID customerId, String key, String fingerprint) {
        IdempotencyRow existing = jdbc.queryForObject("""
                SELECT request_fingerprint, payment_id
                  FROM idempotency_records
                 WHERE customer_id = ? AND idempotency_key = ?
                """, (rs, rowNum) -> new IdempotencyRow(
                rs.getString("request_fingerprint"), rs.getObject("payment_id", UUID.class)),
                customerId, key);

        if (!existing.fingerprint().equals(fingerprint)) {
            throw new IllegalStateException("IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_REQUEST");
        }
        if (existing.paymentId() == null) {
            // A committed incomplete row should not normally be possible because
            // reservation and completion share one transaction.
            throw new IllegalStateException("IDEMPOTENCY_RESULT_INCOMPLETE");
        }
        return jdbc.queryForObject("""
                SELECT id, source_account_id, destination_account_id, amount, currency, status, created_at
                  FROM payments WHERE id = ?
                """, TRANSFER_ROW_MAPPER, existing.paymentId());
    }

    private Map<UUID, AccountRow> lockAccounts(UUID sourceId, UUID destinationId) {
        // Lock in stable UUID order to reduce deadlock risk for opposite transfers.
        return jdbc.query("""
                SELECT id, customer_id, currency, balance, status
                  FROM accounts
                 WHERE id IN (?, ?)
                 ORDER BY id
                 FOR UPDATE
                """, rs -> {
            Map<UUID, AccountRow> rows = new java.util.HashMap<>();
            while (rs.next()) {
                AccountRow row = new AccountRow(
                        rs.getObject("id", UUID.class),
                        rs.getObject("customer_id", UUID.class),
                        rs.getString("currency").trim(),
                        rs.getBigDecimal("balance"),
                        rs.getString("status"));
                rows.put(row.id(), row);
            }
            return rows;
        }, sourceId, destinationId);
    }

    private static void requireValidRequest(UUID customerId, UUID sourceId, UUID destinationId,
                                            Money amount, String key, String fingerprint) {
        if (customerId == null || sourceId == null || destinationId == null || amount == null) {
            throw new IllegalArgumentException("TRANSFER_FIELDS_REQUIRED");
        }
        if (sourceId.equals(destinationId)) {
            throw new IllegalArgumentException("SOURCE_AND_DESTINATION_MUST_DIFFER");
        }
        if (key == null || key.isBlank() || key.length() > 255) {
            throw new IllegalArgumentException("INVALID_IDEMPOTENCY_KEY");
        }
        if (fingerprint == null || !fingerprint.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalArgumentException("INVALID_REQUEST_FINGERPRINT");
        }
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize transfer payload", e);
        }
    }

    private record AccountRow(UUID id, UUID customerId, String currency,
                              BigDecimal balance, String status) {}
    private record IdempotencyRow(String fingerprint, UUID paymentId) {}

    private static final RowMapper<BankTransfer> TRANSFER_ROW_MAPPER = (rs, rowNum) -> mapTransfer(rs);

    private static BankTransfer mapTransfer(ResultSet rs) throws SQLException {
        Money amount = new Money(rs.getBigDecimal("amount"),
                Currency.getInstance(rs.getString("currency").trim()));
        return new BankTransfer(
                rs.getObject("id", UUID.class),
                rs.getObject("source_account_id", UUID.class),
                rs.getObject("destination_account_id", UUID.class),
                amount,
                BankTransfer.Status.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant());
    }
}
