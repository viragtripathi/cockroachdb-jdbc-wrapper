package com.cockroachdb.jdbc;

import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.core.IntervalFunction;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RetryableExecutorCustomConfigTest {

    @Test
    void testCustomRetryConfigWithExponentialBackoff() {
        AtomicInteger attempts = new AtomicInteger(0);

        RetryConfig customConfig = RetryConfig.custom()
                .maxAttempts(4) // custom: fewer attempts
                .waitDuration(Duration.ofMillis(200)) // base wait
                .intervalFunction(IntervalFunction.ofExponentialBackoff()) // exponential backoff
                .retryOnException(RetryableExecutor::isRetryable)
                .build();

        RetryableExecutor executor = new RetryableExecutor(customConfig);

        try {
            executor.executeVoid(() -> {
                if (attempts.incrementAndGet() < 3) {
                    throw new RuntimeException(new SQLException("Simulate retry", "40001"));
                }
            });
        } catch (SQLException e) {
            fail("Should not have failed with sufficient retry attempts");
        }

        assertEquals(3, attempts.get(), "Expected 2 retries before succeeding on attempt 3");
    }
}
