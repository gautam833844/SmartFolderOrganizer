# Changelog

All notable changes to the **SmartFolderOrganizer** project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-07-28

### Added
- **Main Dashboard**: Enterprise UI shell with dynamic navigation bar, stat cards, and status bar.
- **Scanner Workspace**: Java NIO directory scanning with category filtering badges and details panel.
- **Preview & Conflict Resolution Workspace**: Dual TreeViews displaying source vs proposed category tree structure and path collision warnings.
- **Organization Progress Workspace**: Real-time progress bar, speed calculator, time remaining estimation, and live activity audit log table.
- **Duplicate File Manager**: Multi-stage duplicate detection using size pre-filtering and parallel streaming SHA-256 checksum hashing.
- **Transaction History & LIFO Undo**: Persistent JSON audit tracking (`~/.smartfolderorganizer/history.json`) and LIFO file restoration engine.
- **Reports Dashboard**: Analytics dashboard featuring JavaFX PieChart, BarChart, and LineChart visualization metrics.
- **Settings & Preferences**: JSON configuration management (`~/.smartfolderorganizer/settings.json`) with path and theme validations.
- **Folder Watcher Engine**: Background directory monitoring utilizing Java NIO `WatchService` with event debouncing.
- **Quality Assurance Tooling**: Configured JaCoCo, SpotBugs, PMD, Checkstyle, Surefire, and Failsafe Maven plugins.
