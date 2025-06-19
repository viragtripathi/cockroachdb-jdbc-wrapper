# CockroachDB JDBC Wrapper

A lightweight, zero-maintenance Java library that wraps your existing PostgreSQL JDBC connection to automatically retry transient failures commonly encountered when using CockroachDB. These include:

- **Serialization failures** (`40001`)
- **Connection issues** (`08001`, `08003`, `08004`, `08006`, `08007`, `08S01`, `57P01`)

---

## 🎯 Purpose

CockroachDB provides strong consistency and serializable isolation, which can lead to expected `40001` transaction retry errors. Instead of pushing that complexity to every application developer or service maintainer, this library provides a simple drop-in solution that:

- **Automatically retries** transactional errors internally
- **Works transparently** with your existing `DataSource`, `Connection`, or `Statement`
- **Requires no proxy**, reflection, or bytecode instrumentation
- **Adds no runtime dependency except for Resilience4j (lightweight)**
- **Includes integration-tested retry logic** with CockroachDB

---

## 🧰 Features

- `RetryableExecutor` with `execute()` and `executeVoid()` methods
- `RetryableDataSource` and `RetryableConnection` wrappers
- Built-in retry logic for SQLState errors known to be retryable
- Simple, self-contained API

---

## 🚀 Quick Start

### 1. Add Dependency

If using the default JAR (lean):

```xml
<dependency>
  <groupId>com.cockroachdb</groupId>
  <artifactId>cockroachdb-jdbc-wrapper</artifactId>
  <version>0.1.0</version>
</dependency>
```

Then manually include:

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

### 💡 Download Options

| File                                     | Description                                      |
|------------------------------------------|--------------------------------------------------|
| `cockroachdb-jdbc-wrapper-0.1.0.jar`     | Slim JAR (no deps, requires Resilience4j + JDBC) |
| `cockroachdb-jdbc-wrapper-0.1.0-all.jar` | Uber JAR with Resilience4j shaded                |

Add the shaded JAR to your classpath if you want plug-and-play functionality.

---

## ✅ Usage Examples

### Wrap Your JDBC Setup

```java
PGSimpleDataSource pgds = new PGSimpleDataSource();
pgds.setUrl("jdbc:postgresql://localhost:26257/defaultdb?sslmode=disable");
pgds.setUser("root");

DataSource retryable = new RetryableDataSource(pgds);
Connection conn = retryable.getConnection();
```

### Automatic Retry of Writes

```java
RetryableExecutor executor = new RetryableExecutor();

executor.executeVoid(() -> {
    try (PreparedStatement ps = conn.prepareStatement("INSERT INTO users (id, name) VALUES (?, ?)")) {
        ps.setInt(1, 1);
        ps.setString(2, "Alice");
        ps.executeUpdate();
    } catch (SQLException e) {
        throw new RuntimeException(e);
    }
});
```

### Automatic Retry of Reads

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

## 🔁 Retry Behavior

The following SQLState codes are retried internally:

- `40001`: Serialization failure (CockroachDB retryable transaction error)
- All `08xxx` codes: Connection-level errors
- `57P01`: Admin shutdown

Backoff and retry behavior is managed by **Resilience4j**, using 5 attempts with 500ms wait time by default.

---

## 🔧 Custom Retry Configuration

You can override the defaults using `RetryConfig`:

```java
RetryConfig config = RetryConfig.custom()
    .maxAttempts(7)
    .waitDuration(Duration.ofMillis(250))
    .retryOnException(RetryableExecutor::isRetryable)
    .build();

RetryableExecutor executor = new RetryableExecutor(config);
```

### 🌀 Use Exponential Backoff

```java
.intervalFunction(IntervalFunction.ofExponentialBackoff())
```

---

## 🧪 Integration Testing with CockroachDB

Use Docker Compose to spin up a local cluster:

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

Start it:
```bash
docker-compose up -d
```

Then run:
```bash
mvn clean test
```

---

## 🧼 Zero Maintenance Philosophy

This library:

- Requires no config or boilerplate
- Uses no static hooks or agent hacks
- Can be upgraded as a single, versioned `.jar`
- Is tested with CockroachDB under containerized conditions

You just:
```java
new RetryableConnection(...) // and forget about retry plumbing
```

---

## 📦 Build and Release

```bash
mvn clean package          # slim JAR
mvn clean package -Puber   # builds cockroachdb-jdbc-wrapper-0.1.0-all.jar
```

Or tag + release via JReleaser CI:
```bash
git tag v0.1.0 && git push origin v0.1.0
```
