# Architectural Design Decisions

This document outlines key technical design decisions made during the architecture of Smart Folder Organizer.

---

## 1. Why Java 21 & JavaFX 21?
- **Java 21 LTS**: Provides modern language capabilities (Records, Pattern Matching, Sequenced Collections, Virtual Threads readiness) with long-term stability and high-performance JVM runtime.
- **JavaFX 21**: Offers enterprise-grade desktop GUI controls, declarative FXML layout separation, hardware-accelerated graphics, CSS styling, and built-in `Task` concurrency APIs.

---

## 2. Why Java NIO (`java.nio.file`) Over Legacy `java.io.File`?
- **High Performance**: Uses non-blocking native filesystem primitives.
- **Atomic Operations**: Supports `StandardCopyOption.ATOMIC_MOVE` to prevent partial file write corruptions.
- **Directory Tree Visitors**: `Files.walkFileTree` enables high-speed recursive scanning without stack overflow risks on deep directory hierarchies.
- **WatchService**: Provides native OS event hooks (`ENTRY_CREATE`, `ENTRY_DELETE`, `ENTRY_MODIFY`).

---

## 3. Why JSON Persistence (Jackson) Over SQL/H2?
- **Lightweight Footprint**: Eliminates the overhead of embedded RDBMS engines.
- **Human-Readable Audits**: Configuration (`settings.json`) and transaction history (`history.json`) can be inspected or backed up using standard text editors.
- **JavaTimeModule Integration**: Provides seamless ISO-8601 date serialization.

---

## 4. Why Multi-Stage SHA-256 Duplicate Detection?
- **Stage 1 (Size Filtering)**: Files with distinct byte sizes can never be duplicates. Discarding unique files by size avoids millions of unnecessary disk reads.
- **Stage 2 (Metadata Grouping)**: Groups files sharing identical size and extension.
- **Stage 3 (Streaming SHA-256 Checksums)**: Only files passing Stages 1 & 2 are hashed using 64KB byte buffer streams, avoiding full memory loads on multi-gigabyte files.

---

## 5. Why LIFO Undo Transactions?
- Reversing operations in Last-In, First-Out order ensures nested directory moves are unwound correctly without missing parent target paths.
