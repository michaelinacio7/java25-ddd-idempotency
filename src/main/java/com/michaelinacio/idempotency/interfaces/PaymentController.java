package com.michaelinacio.idempotency.interfaces;

import com.michaelinacio.idempotency.application.payment.CreatePaymentCommand;
import com.michaelinacio.idempotency.application.payment.CreatePaymentService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Currency;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    private final CreatePaymentService service;

    public PaymentController(CreatePaymentService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<String> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @RequestBody(required = false) Request request) {
        if (request == null || request.amount() == null || request.currency() == null || request.currency().isBlank()) {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"INVALID_PAYMENT\"}");
        }

        final Currency currency;
        try {
            currency = Currency.getInstance(request.currency().trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"INVALID_CURRENCY\"}");
        }

        var result = service.execute(key, new CreatePaymentCommand(request.amount(), currency));
        return ResponseEntity.status(result.status())
                .header("Idempotency-Key", key == null ? "" : key)
                .header("Idempotency-Replayed", Boolean.toString(result.replayed()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(result.response());
    }

    public record Request(java.math.BigDecimal amount, String currency) {}
}