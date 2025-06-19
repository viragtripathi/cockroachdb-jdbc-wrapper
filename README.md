[![Build](https://github.com/viragtripathi/cockroachdb-jdbc-wrapper/actions/workflows/ci.yml/badge.svg)](https://github.com/viragtripathi/cockroachdb-jdbc-wrapper/actions/workflows/ci.yml)
[![GitHub release](https://img.shields.io/github/v/release/viragtripathi/cockroachdb-jdbc-wrapper)](https://github.com/viragtripathi/cockroachdb-jdbc-wrapper/releases)
[![License](https://img.shields.io/github/license/viragtripathi/cockroachdb-jdbc-wrapper)](https://github.com/viragtripathi/cockroachdb-jdbc-wrapper/blob/main/LICENSE)

# CockroachDB JDBC Wrapper

A lightweight, zero-maintenance Java library that wraps your existing PostgreSQL JDBC connection to automatically retry transient failures commonly encountered when using CockroachDB. These include:

- [**Serialization failures**](https://github.com/pgjdbc/pgjdbc/blob/master/pgjdbc/src/main/java/org/postgresql/util/PSQLState.java#L86) (`40001`)
- [**Connection issues**](https://github.com/pgjdbc/pgjdbc/blob/master/pgjdbc/src/main/java/org/postgresql/util/PSQLState.java) (`08001`, `08003`, `08004`, `08006`, `08007`, `08S01`, `57P01`)

---

## 🎯 Purpose

CockroachDB provides strong consistency and serializable isolation, which can lead to expected `40001` transaction retry errors. Instead of pushing that complexity to every application developer or service maintainer, this library provides a simple, non-intrusive solution that:

- **Automatically retries** transactional errors internally
- **Works transparently** with your existing JDBC logic
- **Adds no proxies**, reflection, bytecode hacks, or driver replacements
- **Includes integration-tested retry logic** with CockroachDB
- **Lets you opt-in** to optional wrapper types if needed

---

## 🧰 Features

- `RetryableExecutor` – simple utility to retry logic with `execute()` and `executeVoid()`
- Optional `RetryableDataSource` and `RetryableConnection` for automatic retry of `commit()` and `rollback()`
- Built-in handling of well-known CockroachDB/SQL state codes
- Supports Resilience4j backoff and retry configuration
- Zero-maintenance: no proxy servers, agents, or instrumentation

---

## 🚀 Quick Start

### 1. Add Dependency

```xml
<dependency>
  <groupId>com.cockroachdb</groupId>
  <artifactId>cockroachdb-jdbc-wrapper</artifactId>
  <version>0.1.0</version>
</dependency>
````

If using the slim JAR, include dependencies manually:

```xml
<dependency>
  <groupId>io.github.resilience4j</groupId>
  <artifactId>resilience4j-core</artifactId>
</dependency>
<dependency>
  <groupId>io.github.resilience4j</groupId>
  <artifactId>resilience4j-retry</artifactId>
</dependency>
<dependency>
  <groupId>org.postgresql</groupId>
  <artifactId>postgresql</artifactId>
</dependency>
```

---

## 💡 Download Options

| File                                     | Description                                      |
|------------------------------------------|--------------------------------------------------|
| `cockroachdb-jdbc-wrapper-0.1.0.jar`     | Slim JAR (no deps, requires Resilience4j + JDBC) |
| `cockroachdb-jdbc-wrapper-0.1.0-all.jar` | Uber JAR with Resilience4j shaded                |

---

## ✅ Usage Examples

### ✅ Recommended (Minimal) – Use `RetryableExecutor`

```java
RetryableExecutor executor = new RetryableExecutor();

executor.executeVoid(() -> {
    try (Connection conn = dataSource.getConnection()) {
        conn.setAutoCommit(false);
        // ... your DB ops
        conn.commit();
    }
});
```

This keeps your code unchanged while centralizing retry logic.

---

### 🧩 Optional: Wrap Your `DataSource` or `Connection`

If you want automatic retries for `commit()` / `rollback()` without calling the executor explicitly:

```java
PGSimpleDataSource pgds = new PGSimpleDataSource();
pgds.setUrl("jdbc:postgresql://localhost:26257/defaultdb?sslmode=disable");
pgds.setUser("root");

DataSource retryable = new RetryableDataSource(pgds);
Connection conn = retryable.getConnection();  // wraps with retry logic for commit/rollback
```

> ✅ These wrappers are safe, pure Java delegations — they don’t override driver behavior, proxy the JDBC layer, or touch internal state.

---

### 🔁 Retry of Reads

```java
int userCount = executor.execute(() -> {
    try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM users");
         ResultSet rs = ps.executeQuery()) {
        rs.next();
        return rs.getInt(1);
    } catch (SQLException e) {
        throw new RuntimeException(e);
    }
});
```

---

## 🔧 Custom Retry Configuration

```java
RetryConfig config = RetryConfig.custom()
    .maxAttempts(7)
    .waitDuration(Duration.ofMillis(250))
    .retryOnException(RetryableExecutor::isRetryable)
    .build();

RetryableExecutor executor = new RetryableExecutor(config);
```

### 🌀 Exponential Backoff

```java
.intervalFunction(IntervalFunction.ofExponentialBackoff())
```

---

## 🔁 Retry Behavior

Retries are automatically triggered for:

* `40001`: Serialization failures
* Any `08XXX`: Connection issues
* `57P01`: Admin shutdown

Backoff is managed via **Resilience4j** and is fully customizable.

---

## 🧪 Integration Testing with CockroachDB

```yaml
version: '3.8'
services:
  cockroach:
    image: cockroachdb/cockroach:latest
    command: start-single-node --insecure
    ports:
      - "26257:26257"
      - "8080:8080"
```

```bash
docker-compose up -d
./mvnw clean test
```
---

## 🧪 Sample Integration Demo

Looking for a complete working example?

👉 Check out the [cockroachdb-jdbc-wrapper-demo](https://github.com/viragtripathi/cockroachdb-jdbc-wrapper-demo) repository.

It shows how to:

- Connect to CockroachDB using the JDBC wrapper
- Use `RetryableExecutor` for clean, retryable read/write logic
- Simulate `40001` errors for live retry demos
- Run everything locally using Docker and Maven

Great for onboarding, testing, and CI!
