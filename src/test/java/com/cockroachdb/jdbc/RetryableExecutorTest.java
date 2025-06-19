package com.cockroachdb.jdbc;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RetryableExecutorTest {

    @Test
    void retriesOnSerializationFailure() {
        RetryableExecutor executor = new RetryableExecutor();
        AtomicInteger attempts = new AtomicInteger(0);

        try {
            executor.executeVoid(() -> {
                if (attempts.incrementAndGet() < 3) {
                    throw new RuntimeException(new SQLException("Simulated serialization failure", "40001"));
                }
            });
        } catch (SQLException e) {
            fail("Retries exhausted unexpectedly: " + e.getMessage());
        }

        assertEquals(3, attempts.get());
    }

    @Test
    void retriesOnConnectionError() {
        RetryableExecutor executor = new RetryableExecutor();
        AtomicInteger attempts = new AtomicInteger(0);

        try {
            executor.executeVoid(() -> {
                if (attempts.incrementAndGet() < 2) {
                    throw new RuntimeException(new SQLException("Connection error", "08006"));
                }
            });
        } catch (SQLException e) {
            fail("Retries exhausted unexpectedly: " + e.getMessage());
        }

        assertEquals(2, attempts.get());
    }

    @Test
    void doesNotRetryOnFatalError() {
        RetryableExecutor executor = new RetryableExecutor();

        SQLException thrown = assertThrows(SQLException.class, () -> {
            executor.executeVoid(() -> {
                throw new RuntimeException(new SQLException("Syntax error", "42601"));
            });
        });

        assertEquals("42601", thrown.getSQLState());
    }

    @Test
    void testExecuteWithReturnValue() {
        RetryableExecutor executor = new RetryableExecutor();
        AtomicInteger attempts = new AtomicInteger(0);

        try {
            int result = executor.execute(() -> {
                if (attempts.incrementAndGet() < 3) {
                    throw new RuntimeException(new SQLException("Temporary read failure", "40001"));
                }
                return 42; // Simulate successful query result
            });

            assertEquals(42, result);
            assertEquals(3, attempts.get());  // should have retried twice before succeeding
        } catch (SQLException e) {
            fail("Retries exhausted unexpectedly: " + e.getMessage());
        }
    }

}
