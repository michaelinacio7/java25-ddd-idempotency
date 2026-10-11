package com.michaelinacio.idempotency.interfaces;

import com.michaelinacio.idempotency.application.payment.TransferCommand;
import com.michaelinacio.idempotency.application.payment.TransferService;
import com.michaelinacio.idempotency.domain.payment.BankTransfer;
import com.michaelinacio.idempotency.infrastructure.JdbcCustomerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {
    private final TransferService transferService;
    private final JdbcCustomerRepository customers;

    public TransferController(TransferService transferService, JdbcCustomerRepository customers) {
        this.transferService = transferService;
        this.customers = customers;
    }

    @PostMapping
    public ResponseEntity<?> transfer(
            Principal principal,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody TransferRequest request) {
        // Customer identity must come from the authenticated principal, never from
        // a caller-controlled customerId field in the JSON body. The deployment
        // must configure its trusted authentication layer to populate Principal.
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("code", "AUTHENTICATION_REQUIRED"));
        }

        UUID customerId = customers.findIdByExternalSubject(principal.getName());
        BankTransfer transfer = transferService.transfer(new TransferCommand(
                customerId, request.sourceAccountId(), request.destinationAccountId(),
                request.amount(), request.currency(), idempotencyKey));

        return ResponseEntity.status(HttpStatus.CREATED).body(new TransferResponse(
                transfer.id(), transfer.sourceAccountId(), transfer.destinationAccountId(),
                transfer.amount().amount(), transfer.amount().currency().getCurrencyCode(),
                transfer.status().name(), transfer.createdAt()));
    }

    public record TransferRequest(UUID sourceAccountId, UUID destinationAccountId,
                                  BigDecimal amount, String currency) {}
    public record TransferResponse(UUID id, UUID sourceAccountId, UUID destinationAccountId,
                                   BigDecimal amount, String currency, String status,
                                   java.time.Instant createdAt) {}
}
