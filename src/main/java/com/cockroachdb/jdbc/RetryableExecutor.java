package com.cockroachdb.jdbc;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.*;
import org.postgresql.util.PSQLState;

import java.sql.SQLException;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Usage Example for Custom Config:
 * RetryConfig config = RetryConfig.custom()
 *      .maxAttempts(10)
 *      .waitDuration(Duration.ofSeconds(1))
 *      .retryOnException(RetryableExecutor::isRetryable)
 *      .build();
 * RetryableExecutor executor = new RetryableExecutor(config);
 */
public class RetryableExecutor {
    private final Retry retry;

    // Default constructor uses sensible defaults
    public RetryableExecutor() {
        this(buildDefaultRetryConfig());
    }

    // Customizable constructor
    public RetryableExecutor(RetryConfig config) {
        this.retry = Retry.of("crdbRetry", config);
    }

    // Build default RetryConfig (5 attempts, exponential backoff with jitter starting at 200ms)
    public static RetryConfig buildDefaultRetryConfig() {
        return RetryConfig.custom()
                .maxAttempts(5)
                .waitDuration(Duration.ofMillis(200))
                .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(200, 2.0, 0.5))
                .retryOnException(RetryableExecutor::isRetryable)
                .build();
    }

    // Determines if an exception is retryable
    public static boolean isRetryable(Throwable t) {
        Throwable cause = t instanceof RuntimeException && t.getCause() instanceof SQLException
                ? t.getCause()
                : t;

        if (!(cause instanceof SQLException sqlEx)) return false;
        String state = sqlEx.getSQLState();
        if (state == null) return false;
        return PSQLState.SERIALIZATION_FAILURE.getState().equals(state)
                || state.startsWith("08")
                || "57P01".equals(state);
    }

    public <T> T execute(Supplier<T> operation) throws SQLException {
        try {
            return Retry.decorateSupplier(retry, operation).get();
        } catch (Exception e) {
            if (e.getCause() instanceof SQLException) {
                throw (SQLException) e.getCause();
            }
            throw e;
        }
    }

    public void executeVoid(Runnable operation) throws SQLException {
        try {
            Retry.decorateRunnable(retry, operation).run();
        } catch (Exception e) {
            if (e.getCause() instanceof SQLException) {
                throw (SQLException) e.getCause();
            }
            throw e;
        }
    }
}
