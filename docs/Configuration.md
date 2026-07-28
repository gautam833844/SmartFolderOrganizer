# Configuration Reference

## Overview

All application configuration properties, default paths, themes, and transaction logs are persisted in JSON format under the user's home directory:
`~/.smartfolderorganizer/`

---

## Configuration Files

### 1. `settings.json`
Stores application preferences managed by `SettingsService`:

```json
{
  "theme": "SYSTEM",
  "windowWidth": 1400.0,
  "windowHeight": 850.0,
  "defaultScanFolder": "C:/Users/Sample/Downloads",
  "defaultDestinationFolder": "C:/Users/Sample/Organized",
  "autoSave": true,
  "scanOptions": {
    "recursive": true,
    "includeHidden": false,
    "followLinks": false
  },
  "organizationOptions": {
    "createDirectories": true,
    "overwriteExisting": false,
    "verifyAfterMove": true
  }
}
```

### 2. `history.json`
Stores immutable transaction history logs managed by `TransactionPersistenceService`.

---

## Supported Themes

- `LIGHT`: Clean modern light theme palette.
- `DARK`: Professional dark mode interface.
- `SYSTEM`: Matches desktop system settings.

---

## Settings Requiring Restart
- **Application Language**: Resource bundle switching requires application restart.
- **Max Scan Threads**: Thread pool boundaries initialize on application launch.
