@echo off
setlocal
set DIR=%~dp0
set GRADLE_VERSION=9.5.1
set GRADLE_HOME=%DIR%.gradle\local\gradle-%GRADLE_VERSION%
if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  powershell -NoProfile -ExecutionPolicy Bypass -Command "New-Item -ItemType Directory -Force -Path '%DIR%.gradle\local' | Out-Null; Invoke-WebRequest -UseBasicParsing 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%DIR%.gradle\local\gradle.zip'; Expand-Archive -Force '%DIR%.gradle\local\gradle.zip' '%DIR%.gradle\local'"
)
call "%GRADLE_HOME%\bin\gradle.bat" %*
