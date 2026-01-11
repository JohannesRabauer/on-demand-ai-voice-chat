# Create Portable Package for On-Demand AI Voice Chat
# Creates a self-contained application with bundled JRE (no installer needed)

Write-Host "Building On-Demand AI Voice Chat Portable Package..." -ForegroundColor Cyan

# Step 1: Build the application
Write-Host "`n[1/3] Building application with Maven..." -ForegroundColor Yellow
mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed!" -ForegroundColor Red
    exit 1
}

# Step 2: Create runtime image with jlink
Write-Host "`n[2/3] Creating custom Java runtime..." -ForegroundColor Yellow

$appImage = "OnDemandAIVoice"
if (Test-Path $appImage) {
    Remove-Item -Recurse -Force $appImage
}

jpackage `
    --type app-image `
    --name "OnDemandAIVoice" `
    --app-version "1.0.0" `
    --vendor "AI Voice Chat" `
    --description "AI-powered voice assistant" `
    --input target/quarkus-app `
    --main-jar quarkus-run.jar `
    --icon Icon_cropped.png `
    --dest . `
    --java-options "-Xmx512m"

if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to create app image!" -ForegroundColor Red
    exit 1
}

# Step 3: Copy resources and create portable package
Write-Host "`n[3/3] Creating portable package..." -ForegroundColor Yellow

# Copy icon to app folder
Copy-Item "Icon_cropped.png" "$appImage\" -ErrorAction SilentlyContinue

# Create launcher script
@"
@echo off
cd /d "%~dp0"
start "" "OnDemandAIVoice.exe"
"@ | Out-File -FilePath "$appImage\Launch.bat" -Encoding ASCII

# Create README
@"
On-Demand AI Voice Chat - Portable Edition
===========================================

To run the application:
1. Double-click "OnDemandAIVoice.exe" or "Launch.bat"
2. The app will start in the system tray
3. Press F8 to start/stop recording

Configuration:
- Edit app/application.properties to set your OpenAI API key
- Change system prompt, voice, and other settings there

Requirements:
- Microphone for audio input
- Speakers for audio output
- Internet connection for OpenAI API

Press F8 to toggle recording. The AI will respond after you stop recording.
"@ | Out-File -FilePath "$appImage\README.txt" -Encoding UTF8

# Create ZIP package
Write-Host "`nCreating ZIP package..." -ForegroundColor Yellow
$zipName = "OnDemandAIVoice-Portable-1.0.0.zip"
if (Test-Path $zipName) {
    Remove-Item $zipName
}

Compress-Archive -Path $appImage -DestinationPath $zipName -CompressionLevel Optimal

$zipSize = (Get-Item $zipName).Length / 1MB

Write-Host "`n✓ Portable package created successfully!" -ForegroundColor Green
Write-Host "`nPackage: $zipName" -ForegroundColor Cyan
Write-Host "Size: $([math]::Round($zipSize, 2)) MB" -ForegroundColor White
Write-Host "Folder: $appImage\" -ForegroundColor Cyan
Write-Host "`nDistribution options:" -ForegroundColor Yellow
Write-Host "  1. Distribute the ZIP file - users extract and run" -ForegroundColor White
Write-Host "  2. Distribute the folder directly" -ForegroundColor White
Write-Host "`nNo installation needed - just extract and run OnDemandAIVoice.exe!" -ForegroundColor Green
