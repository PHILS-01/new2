@echo off
setlocal
cd /d "%~dp0"
if exist .gradle rmdir /s /q .gradle
if exist app\build rmdir /s /q app\build
if exist local.properties del /q local.properties
if exist gradlew.bat (
  call gradlew.bat clean assembleDebug
) else (
  echo Gradle wrapper JAR/scripts are not included in this project.
  echo Open this folder in Android Studio, allow Gradle sync, then choose Build ^> Clean Project and Build ^> Rebuild Project.
)
endlocal
