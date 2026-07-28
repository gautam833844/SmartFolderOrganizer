# Duplicate Detection Engine

## Overview

`DuplicateDetectionService` identifies duplicate files using a multi-stage filtering pipeline.

---

## Detection Pipeline

```mermaid
graph TD
    Files["Input FileItems"] --> Stage1["Stage 1: Minimum Size Filter"]
    Stage1 --> Stage2["Stage 2: Metadata Pre-Grouping (Size & Ext)"]
    Stage2 --> Filter1["Filter Groups < 2 Items"]
    Filter1 --> Stage3["Stage 3: Parallel Streaming Cryptographic Hash (SHA-256)"]
    Stage3 --> Groups["Construct DuplicateGroup Entities"]
```

---

## Supported Hash Algorithms
- `SHA-256` (Default, recommended)
- `SHA-1`
- `MD5`
