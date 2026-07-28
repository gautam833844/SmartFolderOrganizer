# LIFO Undo Engine Specification

## Overview

`UndoService` reverses completed organization transactions, returning files back to their original source directories.

---

## Undo Execution Sequence

```mermaid
sequenceDiagram
    participant UI as HistoryController
    participant Undo as UndoService
    participant Disk as Local FileSystem
    participant Hist as TransactionHistory

    UI->>Undo: undo(transaction, listener)
    activate Undo
    Undo->>Undo: Reverse Operations Order (LIFO)
    loop For Each MoveOperation
        Undo->>Disk: Recreate Original Directory (if missing)
        Undo->>Disk: Files.move(destPath, sourcePath)
        Disk-->>Undo: Verification
    end
    Undo->>Hist: removeTransaction(transactionId)
    Undo-->>UI: Return UndoResult
    deactivate Undo
```

---

## Fault Tolerance & Continuation
If an organized file was manually deleted or locked post-organization, `UndoService` logs the failure in `UndoResult.getErrors()`, triggers `listener.onFileSkipped()`, and continues restoring all remaining valid files in the transaction.
