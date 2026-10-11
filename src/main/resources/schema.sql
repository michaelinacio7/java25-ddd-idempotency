CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY,
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL
);

CREATE TABLE IF NOT EXISTS idempotency_records (
    idempotency_key VARCHAR(255) PRIMARY KEY,
    fingerprint VARCHAR(64) NOT NULL,
    status INT NOT NULL,
    response CLOB
);