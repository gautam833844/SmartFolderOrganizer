# Deployment & Packaging Guide

## Requirements
- Java 21 LTS JDK
- Apache Maven 3.8+

---

## Building Executable Fat JAR

Build the standalone executable JAR using the Maven Shade plugin:
```bash
mvn clean package
```
Output artifact: `target/SmartFolderOrganizer-1.0.0-SNAPSHOT.jar`

---

## Native OS Packaging with `jpackage`

To generate native Windows installers (`.msi` / `.exe`):
```bash
jpackage --type msi \
  --name "SmartFolderOrganizer" \
  --app-version "1.0.0" \
  --input target/ \
  --main-jar SmartFolderOrganizer-1.0.0-SNAPSHOT.jar \
  --main-class com.smartfolderorganizer.app.AppLauncher \
  --win-shortcut \
  --win-menu
```
