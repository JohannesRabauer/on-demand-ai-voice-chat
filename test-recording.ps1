#!/usr/bin/env pwsh
# Quick test script to validate recording flow

Write-Host "=== Testing Audio Recording Flow ===" -ForegroundColor Cyan
Write-Host "Starting recording for 3 seconds (speak into microphone)..." -ForegroundColor Yellow

# Launch the app in background
$proc = Start-Process -FilePath "java" -ArgumentList "-jar","target/quarkus-app/quarkus-run.jar" -PassThru -WindowStyle Hidden

Start-Sleep -Seconds 2

Write-Host "Recording will be triggered manually through tray. Press Ctrl+C when done." -ForegroundColor Green

# Keep script alive
try {
    Wait-Process -Id $proc.Id
} catch {
    Write-Host "Process ended." -ForegroundColor Red
}
