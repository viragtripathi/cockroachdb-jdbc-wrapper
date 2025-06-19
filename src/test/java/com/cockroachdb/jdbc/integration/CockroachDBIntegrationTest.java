package com.cockroachdb.jdbc.integration;

import com.cockroachdb.jdbc.RetryableConnection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CockroachDBIntegrationTest {

    private static final String jdbcUrl = "jdbc:postgresql://localhost:26257/defaultdb?sslmode=disable";
    private static final String user = "root";
    private static final String password = "";

    @BeforeAll
    static void waitForDatabase() throws InterruptedException {
        int retries = 10;
        while (retries-- > 0) {
            try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password)) {
                return;
            } catch (SQLException e) {
                Thread.sleep(1000);
            }
        }
        throw new RuntimeException("CockroachDB is not available at " + jdbcUrl);
    }

    @Test
    void testInsertAndRead() throws SQLException {
        try (Connection conn = new RetryableConnection(DriverManager.getConnection(jdbcUrl, user, password))) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS accounts (id INT PRIMARY KEY, balance DECIMAL)");
            }

            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO accounts VALUES (?, ?) ON CONFLICT DO NOTHING")) {
                ps.setInt(1, 1);
                ps.setBigDecimal(2, new BigDecimal("100.00"));
                ps.executeUpdate();
            }
            conn.commit();

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT balance FROM accounts WHERE id = 1")) {
                rs.next();
                BigDecimal actual = rs.getBigDecimal(1);
                BigDecimal expected = new BigDecimal("100.00");
                assertEquals(0, actual.compareTo(expected), "Balance mismatch: expected 100.00 but got " + actual);
            }
        }
    }
}
