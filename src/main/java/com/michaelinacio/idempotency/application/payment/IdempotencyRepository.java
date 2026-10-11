package com.michaelinacio.idempotency.application.payment;

import java.util.Optional;

public interface IdempotencyRepository {
    Optional<Record> find(String key);
    boolean reserve(String key, String fingerprint);
    void complete(String key, int status, String response);

    record Record(String key, String fingerprint, int status, String response) {
        public boolean completed() { return response != null; }
    }
}