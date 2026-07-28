# Contributing to SmartFolderOrganizer

Thank you for considering contributing to **SmartFolderOrganizer**! We welcome bug reports, feature suggestions, and pull requests.

## How to Contribute

### 1. Reporting Issues
- Check existing GitHub Issues to avoid duplicate reports.
- Provide step-by-step instructions, operating system version, and relevant stack traces when submitting bug reports.

### 2. Pull Request Workflow
1. Fork the repository and create a feature branch (`git checkout -b feature/amazing-feature`).
2. Adhere to existing Java 21 formatting and architectural patterns (MVC, SOLID).
3. Ensure all unit tests pass (`mvn clean test`).
4. Ensure code coverage thresholds are maintained (`mvn test jacoco:report`).
5. Run static analysis checks (`mvn spotbugs:check pmd:check checkstyle:check`).
6. Commit changes with clean git messages and submit a Pull Request against `main`.

## Code Style & Standards
- Follow standard Java Naming Conventions.
- Maintain strict MVC separation (UI logic in controllers, business logic in domain services).
- Avoid blocking the JavaFX Application Thread; use JavaFX `Task` for background I/O.
