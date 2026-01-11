# On-Demand AI Voice Chat - Windows Installer Script (EXE version)
# Creates a self-contained executable installer

Write-Host "Building On-Demand AI Voice Chat EXE Installer..." -ForegroundColor Cyan

# Build the application
Write-Host "`n[1/3] Building application..." -ForegroundColor Yellow
mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed!" -ForegroundColor Red
    exit 1
}

# Prepare installer directory
Write-Host "`n[2/3] Preparing files..." -ForegroundColor Yellow
$installerDir = "installer"
if (Test-Path $installerDir) {
    Remove-Item -Recurse -Force $installerDir
}
New-Item -ItemType Directory -Path $installerDir | Out-Null
Copy-Item -Recurse "target/quarkus-app/*" "$installerDir/"
Copy-Item "Icon_cropped.png" "$installerDir/" -ErrorAction SilentlyContinue

# Create EXE installer
Write-Host "`n[3/3] Creating EXE installer..." -ForegroundColor Yellow

jpackage `
    --type exe `
    --name "OnDemandAIVoice" `
    --app-version "1.0.0" `
    --vendor "AI Voice Chat" `
    --description "AI-powered voice assistant with F8 hotkey control" `
    --input installer `
    --main-jar quarkus-run.jar `
    --icon Icon_cropped.png `
    --dest installers `
    --win-menu `
    --win-shortcut `
    --win-dir-chooser `
    --win-menu-group "AI Voice Chat" `
    --java-options "-Xmx512m"

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n✓ EXE Installer created successfully!" -ForegroundColor Green
    Get-ChildItem installers\*.exe | ForEach-Object {
        Write-Host "Location: $($_.FullName)" -ForegroundColor Cyan
        Write-Host "Size: $([math]::Round($_.Length / 1MB, 2)) MB" -ForegroundColor White
    }
} else {
    Write-Host "`nFailed to create installer!" -ForegroundColor Red
}
