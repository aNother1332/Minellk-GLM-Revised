@echo off
rem Minellk - build and run (requires JDK 11+, JavaFX bundled in lib\javafx)
cd /d "%~dp0"

if not exist out mkdir out
setlocal enabledelayedexpansion
set "SRC="
for /r src %%f in (*.java) do set "SRC=!SRC! "%%f""

javac -encoding UTF-8 --module-path "lib\javafx" --add-modules javafx.controls -d out !SRC!
if errorlevel 1 (
    echo.
    echo Build failed: make sure JDK 11+ is installed and you are in the project root.
    pause
    exit /b 1
)

java --module-path "lib\javafx" --add-modules javafx.controls -cp out app.Main
