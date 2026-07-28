# Architecture Specification

## Overview

Smart Folder Organizer is designed following **Clean Architecture**, **MVC (Model-View-Controller)**, and **SOLID** design principles. The software strictly isolates user interface presentation, domain business rules, filesystem operations, and persistence engines.

---

## Architectural Layers

```mermaid
graph TD
    UI["View Layer (FXML & CSS)"] --> Controller["Controller Layer (JavaFX MVC)"]
    Controller --> Navigation["NavigationManager & ScreenLoader"]
    Controller --> Service["Service Layer (Business Logic Engine)"]
    Service --> Model["Model Layer (Domain Entities)"]
    Service --> Persistence["Persistence Layer (Jackson JSON)"]
    Service --> NIO["FileSystem Layer (Java NIO)"]
```

### 1. View Layer (`src/main/resources/fxml`, `src/main/resources/css`)
- Declarative layout defined using FXML markup files (`MainDashboard.fxml`, `ScannerView.fxml`, `PreviewView.fxml`, etc.).
- Styling isolated in Vanilla CSS stylesheets (`Dashboard.css`, `Scanner.css`, `Preview.css`, etc.) with zero inline FXML style attributes.

### 2. Controller Layer (`com.smartfolderorganizer.controller`)
- JavaFX Controllers coordinate UI controls, data bindings, and background tasks.
- **Strict Constraint**: Controllers contain no direct business logic, file I/O operations, or cryptographic hash processing.

### 3. Service Layer (`com.smartfolderorganizer.service`)
- Enforces single responsibility domain services:
  - `ScanService`: Non-blocking directory tree visitors.
  - `PreviewService`: Target destination path mapping and collision checks.
  - `OrganizationService`: Physical NIO atomic file movement engine.
  - `DuplicateDetectionService`: Multi-stage duplicate detection engine.
  - `UndoService`: LIFO transaction reversal engine.
  - `FolderWatchService` & `FolderWatcherManager`: Real-time NIO `WatchService` directory monitoring.

### 4. Persistence Layer (`com.smartfolderorganizer.persistence`)
- Handles thread-safe JSON serialization/deserialization via Jackson:
  - `SettingsService`: Manages `~/.smartfolderorganizer/settings.json`.
  - `TransactionPersistenceService`: Manages `~/.smartfolderorganizer/history.json`.

---

## Threading Architecture

```mermaid
sequenceDiagram
    participant UI as JavaFX Application Thread
    participant Task as JavaFX Task Worker
    participant Service as Domain Service
    participant Disk as Local FileSystem

    UI->>Task: Dispatch Background Task
    activate Task
    Task->>Service: Execute I/O Operation
    activate Service
    Service->>Disk: Perform NIO Read/Write
    Disk-->>Service: Return IO Data
    Service-->>Task: Return Result
    deactivate Service
    Task-->>UI: Platform.runLater(Update UI)
    deactivate Task
```

- **JavaFX Application Thread**: Responsible solely for rendering UI scenes and receiving user input events.
- **Worker Daemon Threads**: All directory scans, SHA-256 hash calculations, file moves, and JSON disk reads execute inside JavaFX `Task<T>` worker threads (`ScanEngine-Worker`, `OrganizationEngine-Worker`, `DuplicateEngine-Worker`, `UndoEngine-Worker`).
