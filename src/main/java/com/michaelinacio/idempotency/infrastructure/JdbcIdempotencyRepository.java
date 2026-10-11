package com.michaelinacio.idempotency.infrastructure;

import com.michaelinacio.idempotency.application.payment.IdempotencyRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JdbcIdempotencyRepository implements IdempotencyRepository {
    private final JdbcTemplate jdbc;

    public JdbcIdempotencyRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public Optional<Record> find(String key) {
        return jdbc.query("SELECT idempotency_key, fingerprint, status, response FROM idempotency_records WHERE idempotency_key = ?",
                rs -> rs.next()
                        ? Optional.of(new Record(rs.getString("idempotency_key"), rs.getString("fingerprint"),
                        rs.getInt("status"), rs.getString("response")))
                        : Optional.empty(), key);
    }

    @Override
    public boolean reserve(String key, String fingerprint) {
        try {
            jdbc.update("INSERT INTO idempotency_records(idempotency_key, fingerprint, status, response) VALUES (?, ?, 0, NULL)",
                    key, fingerprint);
            return true;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }

    @Override
    public void complete(String key, int status, String response) {
        jdbc.update("UPDATE idempotency_records SET status = ?, response = ? WHERE idempotency_key = ?",
                status, response, key);
    }
}