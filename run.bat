@echo off
REM Simple launcher for On-Demand AI Voice Chat
REM Assumes Java 21+ is installed

cd /d "%~dp0"

REM Check if Icon exists
if not exist "Icon_cropped.png" (
    echo Warning: Icon file not found
)

REM Launch the application
echo Starting On-Demand AI Voice Chat...
java -Xmx512m -jar target\quarkus-app\quarkus-run.jar

if errorlevel 1 (
    echo.
    echo Error: Failed to start application
    echo Make sure Java 21 or higher is installed
    pause
)
