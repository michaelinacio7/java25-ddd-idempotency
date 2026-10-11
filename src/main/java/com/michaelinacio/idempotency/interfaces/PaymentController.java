package com.michaelinacio.idempotency.interfaces;

import com.michaelinacio.idempotency.application.payment.CreatePaymentCommand;
import com.michaelinacio.idempotency.application.payment.CreatePaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Currency;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    private final CreatePaymentService service;

    public PaymentController(CreatePaymentService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<String> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @RequestBody Request request) {
        try {
            var command = new CreatePaymentCommand(request.amount(), Currency.getInstance(request.currency()));
            var result = service.execute(key, command);
            return ResponseEntity.status(result.status())
                    .header("Idempotency-Key", key == null ? "" : key)
                    .header("Idempotency-Replayed", Boolean.toString(result.replayed()))
                    .header("Content-Type", "application/json")
                    .body(result.response());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body("{\"error\":\"INVALID_PAYMENT\"}");
        }
    }

    public record Request(BigDecimal amount, String currency) {}
}