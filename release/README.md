# SmartFolderOrganizer v1.0.0 Release Package

This directory contains the official release artifacts, documentation, and deployment guides for **SmartFolderOrganizer v1.0.0**.

---

## Contents

- `ReleaseNotes-v1.0.0.md`: Release notes, feature summary, and known limitations.
- `package-windows.bat`: Windows batch script for executing Maven Shade and `jpackage` bundling.
- `README.md`: Application overview and installation guide.
- `LICENSE`: MIT License file.
- `CHANGELOG.md`: Full version history.

---

## Building Native Windows Installer (.msi)

Run the release script on Windows with JDK 21 and WIX Toolset installed:
```cmd
cd release
package-windows.bat
```
