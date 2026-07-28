# SmartFolderOrganizer v1.0.0 — Official Production Release Notes

**Release Date**: July 28, 2026  
**License**: MIT License  
**Target Platform**: Windows 10 / Windows 11 (64-bit)  
**Runtime Requirement**: Bundled Java 21 LTS Runtime  

---

## Executive Summary

**SmartFolderOrganizer v1.0.0** is the initial major production release of the enterprise-grade desktop file organization application. Built on Java 21 LTS and JavaFX 21, the application combines Clean Architecture, MVC separation, non-blocking asynchronous JavaFX `Task` threading, Java NIO file operations, multi-stage cryptographic duplicate detection, transaction audit logs, LIFO undo restoration, and real-time directory watching into a modern desktop experience.

---

## Key Highlights & Feature Matrix

### 1. Intelligent Scanner & Dry-Run Preview
- Non-blocking NIO file visitor scanner with category classification (`Images`, `Documents`, `PDFs`, `Videos`, `Audio`, `Archives`, `Code`, `Others`).
- Dual TreeViews rendering current folder hierarchy versus proposed organized category paths.
- Automatic path collision detection and destination warning alerts via `ConflictDetector`.

### 2. Multi-Stage Duplicate Detection Engine
- Stage 1 file size pre-filtering to eliminate non-duplicate reads.
- Stage 2 structural metadata grouping.
- Stage 3 parallel multi-threaded cryptographic hashing (`SHA-256`, `SHA-1`, `MD5`) using 64KB streaming buffers.
- Selection strategies: `Keep Oldest`, `Keep Newest`, `Select All`, `Clear`.

### 3. Atomic Physical Move Engine & LIFO Undo System
- Atomic NIO file moves (`Files.move()`) with destination directory auto-creation and post-move file integrity verification.
- Transaction audit tracking persisting session logs to `~/.smartfolderorganizer/history.json`.
- Single-click LIFO undo restoration returning organized files back to their original source directories.

### 4. Real-Time Folder Watcher Engine
- Automated background directory monitoring utilizing Java NIO `WatchService`.
- Event debouncing (500 ms) preventing duplicate triggers on rapid OS file writes.
- Automatic organization pipeline reusing existing domain services.

### 5. Analytics & Dashboard Metrics
- Dynamic JavaFX charts (PieChart, BarChart, LineChart) visualization.
- Analytics metrics for files organized, storage saved, duplicate counts, and historical throughput trends.
- Export options: CSV summary and JSON history exports.

---

## Installation & Deployment

### Windows Installer (`.msi` / `.exe`)
Download `SmartFolderOrganizer-1.0.0.msi` and run the setup wizard.
The installer automatically:
- Installs to `C:\Program Files\SmartFolderOrganizer\`
- Registers Desktop and Start Menu shortcuts
- Configures Windows Control Panel uninstall entries
- Bundles a minimal Java 21 JRE runtime (no separate Java installation required)

---

## Known Limitations & Operating System Notes
- **Monitored Directory Locking**: Native Java NIO `WatchService` on Windows maintains directory handles on actively watched folders, preventing external folder renames while active.
- **Cross-Volume Atomic Moves**: Moving files across distinct physical drive letters (e.g. `C:` to `D:`) automatically falls back to copy-and-delete operations.

---

## Future Roadmap
- **Cloud Storage Integration**: Support for Google Drive and OneDrive sync folders.
- **Custom Rule Builder**: User-defined regex and metadata categorization rules.
