# Create Windows Installer for On-Demand AI Voice Chat
# Requires JDK 21+ with jpackage

Write-Host "Building On-Demand AI Voice Chat Installer..." -ForegroundColor Cyan

# Step 1: Build the application
Write-Host "`n[1/4] Building application with Maven..." -ForegroundColor Yellow
mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed!" -ForegroundColor Red
    exit 1
}

# Step 2: Prepare icon (convert PNG to ICO for Windows)
Write-Host "`n[2/4] Preparing icon..." -ForegroundColor Yellow
$iconPath = "Icon_cropped.png"
if (-not (Test-Path $iconPath)) {
    Write-Host "Warning: Icon file not found at $iconPath" -ForegroundColor Yellow
}

# Step 3: Create installer directory structure
Write-Host "`n[3/4] Creating installer structure..." -ForegroundColor Yellow
$installerDir = "installer"
if (Test-Path $installerDir) {
    Remove-Item -Recurse -Force $installerDir
}
New-Item -ItemType Directory -Path $installerDir | Out-Null

# Copy application and resources
Copy-Item -Recurse "target/quarkus-app/*" "$installerDir/"
Copy-Item $iconPath "$installerDir/" -ErrorAction SilentlyContinue

# Copy tray icons (as backup, they're also bundled in the JAR)
Copy-Item "src\main\resources\icons\Icon_small.png" "$installerDir\" -ErrorAction SilentlyContinue
Copy-Item "src\main\resources\icons\Icon_small_rec.png" "$installerDir\" -ErrorAction SilentlyContinue
Copy-Item "src\main\resources\icons\Icon_small_busy.png" "$installerDir\" -ErrorAction SilentlyContinue

# Step 4: Create installer with jpackage
Write-Host "`n[4/4] Creating Windows installer..." -ForegroundColor Yellow

$appVersion = "1.0.0"
$appName = "OnDemandAIVoice"
$vendor = "AI Voice Chat"
$description = "AI-powered voice assistant with hotkey control"

# Find jpackage
$jpackage = Get-Command jpackage -ErrorAction SilentlyContinue
if (-not $jpackage) {
    Write-Host "Error: jpackage not found. Make sure JDK 21+ is installed and in PATH" -ForegroundColor Red
    exit 1
}

# Check for WiX (needed for MSI)
$hasWiX = $null -ne (Get-Command candle.exe -ErrorAction SilentlyContinue) -and `
          $null -ne (Get-Command light.exe -ErrorAction SilentlyContinue)

if (-not $hasWiX) {
    Write-Host "WiX Toolset not found. Creating EXE installer instead of MSI..." -ForegroundColor Yellow
    Write-Host "(To create MSI, install WiX from https://wixtoolset.org and add to PATH)" -ForegroundColor Gray
    $installerType = "exe"
    $installerExt = "exe"
} else {
    Write-Host "WiX Toolset found. Creating MSI installer..." -ForegroundColor Green
    $installerType = "msi"
    $installerExt = "msi"
}

# Create installer
jpackage `
    --type $installerType `
    --name "$appName" `
    --app-version "$appVersion" `
    --vendor "$vendor" `
    --description "$description" `
    --input installer `
    --main-jar quarkus-run.jar `
    --icon $iconPath `
    --dest installers `
    --win-menu `
    --win-shortcut `
    --win-dir-chooser `
    --win-menu-group "AI Voice Chat" `
    --java-options "-Xmx512m" `
    --java-options "-Dfile.encoding=UTF-8"

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n✓ Installer created successfully!" -ForegroundColor Green
    $installerFile = Get-ChildItem "installers\*.$installerExt" | Select-Object -First 1
    if ($installerFile) {
        Write-Host "Location: $($installerFile.FullName)" -ForegroundColor Cyan
        Write-Host "Size: $([math]::Round($installerFile.Length / 1MB, 2)) MB" -ForegroundColor White
        Write-Host "`nYou can now distribute this $($installerExt.ToUpper()) file to install the application on Windows systems." -ForegroundColor White
    }
} else {
    Write-Host "`nFailed to create installer!" -ForegroundColor Red
    Write-Host "Check the error messages above for details." -ForegroundColor Yellow
    exit 1
}
