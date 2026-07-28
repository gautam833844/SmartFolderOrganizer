@echo off
echo ===================================================
echo   SmartFolderOrganizer Native Packaging Script
echo ===================================================
echo.

cd ..
echo [1/3] Building Executable Fat JAR using Maven...
call mvn clean package -DskipTests

if not exist "target\SmartFolderOrganizer-1.0.0-SNAPSHOT.jar" (
    echo Error: Maven build failed. Fat JAR not found.
    exit /b 1
)

echo [2/3] Preparing Release Folder...
if not exist "release\output" mkdir "release\output"

echo [3/3] Executing jpackage for Native Windows Installer...
jpackage ^
  --type msi ^
  --name "SmartFolderOrganizer" ^
  --app-version "1.0.0" ^
  --vendor "SmartFolderOrganizer" ^
  --description "Enterprise Desktop Application for Intelligent File Organization" ^
  --input target\ ^
  --main-jar SmartFolderOrganizer-1.0.0-SNAPSHOT.jar ^
  --main-class com.smartfolderorganizer.app.AppLauncher ^
  --win-shortcut ^
  --win-menu ^
  --win-dir-chooser ^
  --dest release\output

echo.
echo ===================================================
echo   Build Successful! Native MSI generated in release/output
echo ===================================================
pause
