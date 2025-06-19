package com.cockroachdb.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

public class RetryableConnection extends DelegatingConnection {
    private final RetryableExecutor executor = new RetryableExecutor();

    public RetryableConnection(Connection delegate) {
        super(delegate);
    }

    @Override
    public void commit() throws SQLException {
        executor.executeVoid(() -> {
            try {
                super.commit();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void rollback() throws SQLException {
        executor.executeVoid(() -> {
            try {
                super.rollback();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }
}