# Testing & Quality Assurance Specification

## Overview

Smart Folder Organizer enforces automated unit, integration, code coverage, and static analysis quality gates.

---

## Test Frameworks & Tools

- **JUnit 5**: Unit and integration test runner (`junit-jupiter-api`, `junit-jupiter-engine`, `junit-jupiter-params`).
- **Mockito**: Mocking framework (`mockito-core`, `mockito-junit-jupiter`).
- **JaCoCo**: Code coverage analysis plugin (`jacoco-maven-plugin`).
- **SpotBugs, PMD, Checkstyle**: Static code analysis plugins.

---

## Filesystem Isolation with `@TempDir`

All I/O tests utilize JUnit 5 `@TempDir` temporary directories. No unit test modifies the user's real file system.

```java
@Test
void shouldScanRecursiveDirectories(@TempDir Path tempDir) throws IOException {
    Path subDir = tempDir.resolve("subfolder");
    Files.createDirectories(subDir);
    Files.writeString(tempDir.resolve("image.jpg"), "fake data");
    
    ScanResult result = scanService.scan(tempDir);
    assertEquals(1, result.getScannedFiles().size());
}
```

---

## Executing Quality Commands

### 1. Run Unit Tests
```bash
mvn clean test
```

### 2. Generate Coverage Report
```bash
mvn test jacoco:report
```
Report destination: `target/site/jacoco/index.html`

### 3. Run Static Analysis Checks
```bash
mvn spotbugs:check pmd:check checkstyle:check
```
