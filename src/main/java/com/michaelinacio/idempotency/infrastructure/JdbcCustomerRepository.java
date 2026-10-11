package com.michaelinacio.idempotency.infrastructure;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class JdbcCustomerRepository {
    private final JdbcTemplate jdbc;

    public JdbcCustomerRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public UUID findIdByExternalSubject(String externalSubject) {
        return jdbc.queryForObject(
                "SELECT id FROM customers WHERE external_subject = ?",
                UUID.class,
                externalSubject);
    }
}
