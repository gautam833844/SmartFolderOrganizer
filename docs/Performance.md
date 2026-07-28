# Performance & Scaling Engineering

## Performance Characteristics

Smart Folder Organizer is optimized for handling large directory trees (100,000+ files) without freezing the application interface.

---

## Optimizations

### 1. Lazy FXML View Loading & Caching
`ScreenLoader` lazily loads FXML views on first navigation request and caches root nodes in memory, avoiding repeated FXML parsing overhead.

### 2. Multi-Stage Duplicate Pre-Filtering
Rather than computing SHA-256 hashes across all scanned files, `DuplicateDetectionService` filters files by byte size first. Hashing is performed only on candidate groups sharing identical file sizes.

### 3. Parallel Hashing Stream
Stage 3 duplicate checksum calculations execute in parallel across CPU cores using `parallelStream()` and 64KB `MessageDigest` streaming buffers.

### 4. Background Concurrency
All I/O bound tasks run on dedicated daemon threads via JavaFX `Task<T>`. UI updates are pushed back using `Platform.runLater()`.
