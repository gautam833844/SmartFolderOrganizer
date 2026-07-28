# CI/CD Pipeline Documentation

## Overview

Smart Folder Organizer uses **GitHub Actions** for Continuous Integration (CI) validation. Every `push` and `pull_request` targeting `main`, `master`, or `develop` branches triggers automated build, test, static analysis, coverage, and packaging validation workflows.

---

## Workflow Stages (`.github/workflows/maven.yml`)

```mermaid
graph TD
    Trigger["Push / Pull Request"] --> Checkout["1. Checkout Repository"]
    Checkout --> SetupJava["2. Setup Java 21 (Temurin)"]
    SetupJava --> Cache["3. Restore Maven Dependency Cache"]
    Cache --> Compile["4. Compile Project (mvn compile)"]
    Compile --> Test["5. Run Unit Tests (mvn test)"]
    Test --> JaCoCo["6. JaCoCo Coverage Report (mvn jacoco:report)"]
    JaCoCo --> SpotBugs["7. SpotBugs Check"]
    SpotBugs --> PMD["8. PMD Static Analysis"]
    PMD --> Checkstyle["9. Checkstyle Verification"]
    Checkstyle --> Package["10. Package Fat JAR (mvn package)"]
    Package --> Artifacts["Upload Build Artifacts"]
```

---

## Pipeline Artifacts

Each workflow run uploads the following pipeline artifacts:
1. **`jacoco-coverage-report`**: HTML code coverage reports (`target/site/jacoco/`).
2. **`surefire-test-reports`**: Test results and XML summaries (`target/surefire-reports/`).
3. **`SmartFolderOrganizer-Executable-JAR`**: Standalone executable Fat JAR (`target/SmartFolderOrganizer-1.0.0-SNAPSHOT.jar`).

---

## Branching & Release Strategy

- **`main`**: Production-ready code. All PRs require green CI status.
- **`develop`**: Active development branch. Tested continuously via CI.
- **Feature Branches** (`feature/*`): Created for isolated enhancements. Tested via PR triggers to `develop`.

---

## Rerunning Failed Pipeline Builds

1. Navigate to the **Actions** tab in the GitHub repository.
2. Select the failed workflow run.
3. Click **Re-run all jobs** in the top right corner.
