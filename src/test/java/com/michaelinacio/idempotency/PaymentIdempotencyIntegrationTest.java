package com.michaelinacio.idempotency;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentIdempotencyIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void sameKeyAndSamePayloadReplaysOriginalResponse() throws Exception {
        int before = jdbc.queryForObject("SELECT COUNT(*) FROM payments", Integer.class);
        String key = "replay-test-001";
        String payload = "{\"amount\":149.90,\"currency\":\"BRL\"}";

        var first = mvc.perform(post("/payments").header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Replayed", "false"))
                .andReturn().getResponse().getContentAsString();

        mvc.perform(post("/payments").header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andExpect(content().json(first));

        org.junit.jupiter.api.Assertions.assertEquals(before + 1,
                jdbc.queryForObject("SELECT COUNT(*) FROM payments", Integer.class));
    }

    @Test
    void sameKeyWithDifferentPayloadReturnsConflict() throws Exception {
        String key = "conflict-test-001";
        mvc.perform(post("/payments").header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":10.00,\"currency\":\"BRL\"}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/payments").header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":20.00,\"currency\":\"BRL\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("IDEMPOTENCY_KEY_REUSED")));
    }

    @Test
    void nonPositiveAmountReturnsBadRequest() throws Exception {
        mvc.perform(post("/payments").header("Idempotency-Key", "invalid-amount-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":0,\"currency\":\"BRL\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingIdempotencyKeyReturnsBadRequest() throws Exception {
        mvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":10.00,\"currency\":\"BRL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("IDEMPOTENCY_KEY_REQUIRED")));
    }
}