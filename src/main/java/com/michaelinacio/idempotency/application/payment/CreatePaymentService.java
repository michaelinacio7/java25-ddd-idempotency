package com.michaelinacio.idempotency.application.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.michaelinacio.idempotency.domain.payment.Payment;
import com.michaelinacio.idempotency.domain.payment.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class CreatePaymentService {
    private final PaymentRepository payments;
    private final IdempotencyRepository idempotency;
    private final ObjectMapper mapper;

    public CreatePaymentService(PaymentRepository payments, IdempotencyRepository idempotency, ObjectMapper mapper) {
        this.payments = payments;
        this.idempotency = idempotency;
        this.mapper = mapper;
    }

    @Transactional
    public Result execute(String key, CreatePaymentCommand command) {
        if (key == null || key.isBlank()) return error(400, "IDEMPOTENCY_KEY_REQUIRED");
        if (command == null || command.amount() == null || command.currency() == null || command.amount().signum() <= 0)
            return error(400, "INVALID_PAYMENT");

        String fingerprint = fingerprint(command);
        var existing = idempotency.find(key);
        if (existing.isPresent()) return replayOrConflict(existing.get(), fingerprint);

        if (!idempotency.reserve(key, fingerprint)) {
            var concurrent = idempotency.find(key);
            if (concurrent.isEmpty()) return error(409, "REQUEST_IN_PROGRESS");
            return replayOrConflict(concurrent.get(), fingerprint);
        }

        Payment payment;
        try {
            payment = payments.save(Payment.create(command.amount(), command.currency()));
        } catch (IllegalArgumentException ex) {
            throw ex;
        }

        String response = write(new PaymentResponse(payment.id().toString(),
                payment.amount().toPlainString(), payment.currency().getCurrencyCode()));
        idempotency.complete(key, 201, response);
        return new Result(201, response, false);
    }

    private Result replayOrConflict(IdempotencyRepository.Record record, String fingerprint) {
        if (!record.fingerprint().equals(fingerprint)) return error(409, "IDEMPOTENCY_KEY_REUSED");
        if (!record.completed()) return error(409, "REQUEST_IN_PROGRESS");
        return new Result(record.status(), record.response(), true);
    }

    private String fingerprint(CreatePaymentCommand command) {
        String normalized = command.amount().stripTrailingZeros().toPlainString() + "|" +
                command.currency().getCurrencyCode();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private String write(PaymentResponse response) {
        try {
            return mapper.writeValueAsString(response);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize payment response", ex);
        }
    }

    private Result error(int status, String code) {
        try {
            return new Result(status, mapper.writeValueAsString(new ErrorResponse(code)), false);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public record Result(int status, String response, boolean replayed) {}
    public record PaymentResponse(String id, String amount, String currency) {}
    public record ErrorResponse(String error) {}
}