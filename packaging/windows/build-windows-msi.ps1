param(
    [switch]$SkipTests,
    [switch]$SkipDesktopShortcut
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$AppName = "EliteSeriesPay"
$Vendor = "EliteSeriesPay"
$InstallDirName = "EliteSeriesPay"
# Stable upgrade UUID for Windows MSI. Keep unchanged across all releases so newer MSI
# installers replace the previous version instead of blocking with "another version installed".
$WinUpgradeUuid = "7C8D9E0F-1A2B-4C3D-8E5F-6A7B8C9D0E1F"

Push-Location $ProjectRoot
try {
    $MavenArgs = @("package")
    if ($SkipTests) {
        $MavenArgs += "-DskipTests"
    }
    Write-Host "Building application JAR..."
    & mvn @MavenArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Maven build failed with exit code $LASTEXITCODE"
    }

    $ProjectVersion = (& mvn help:evaluate "-Dexpression=project.version" "-DforceStdout" "-q").Trim()
    $ProjectArtifactId = (& mvn help:evaluate "-Dexpression=project.artifactId" "-DforceStdout" "-q").Trim()
    $AppVersion = $ProjectVersion -replace "-SNAPSHOT$", ""
    if ($AppVersion -notmatch '^\d+(\.\d+){0,3}$') {
        throw "MSI app-version must be a dotted numeric version (for example 0.0.2). Current: $AppVersion"
    }
    $MainJar = "$ProjectArtifactId-$ProjectVersion.jar"
    $BuildOutputDir = Join-Path $ProjectRoot "target"
    $InputDir = Join-Path $BuildOutputDir "jpackage-input"
    $OutputDir = Join-Path $BuildOutputDir "dist"
    $MainJarPath = Join-Path $BuildOutputDir $MainJar
    $SpringBootMainClass = "org.springframework.boot.loader.launch.JarLauncher"

    if (-not (Test-Path $MainJarPath)) {
        throw "Application JAR not found: $MainJarPath"
    }

    $OriginalJarPath = "$MainJarPath.original"
    if (-not (Test-Path $OriginalJarPath)) {
        Write-Warning "Expected repackaged Spring Boot JAR marker not found: $OriginalJarPath"
    }

    $ManifestTempDir = Join-Path $env:TEMP ("esp-jar-manifest-" + [Guid]::NewGuid().ToString())
    New-Item -ItemType Directory -Path $ManifestTempDir -Force | Out-Null
    try {
        Push-Location $ManifestTempDir
        & jar xf $MainJarPath META-INF/MANIFEST.MF
        $ManifestPath = Join-Path $ManifestTempDir "META-INF\MANIFEST.MF"
        if (-not (Test-Path $ManifestPath)) {
            throw "Unable to read manifest from $MainJarPath"
        }
        $ManifestContent = Get-Content $ManifestPath -Raw
    }
    finally {
        Pop-Location
        Remove-Item $ManifestTempDir -Recurse -Force -ErrorAction SilentlyContinue
    }

    if ($ManifestContent -notmatch "Main-Class:\s*$([regex]::Escape($SpringBootMainClass))") {
        throw "Unexpected Main-Class in $MainJarPath. Expected Spring Boot executable JAR with Main-Class: $SpringBootMainClass"
    }

    if (Test-Path $InputDir) {
        Remove-Item $InputDir -Recurse -Force
    }
    New-Item -ItemType Directory -Path $InputDir | Out-Null
    Copy-Item $MainJarPath (Join-Path $InputDir $MainJar)

    if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) {
        throw "jpackage was not found. Install JDK 21 and ensure jpackage is on PATH."
    }

    $IconPath = Join-Path $ProjectRoot "installer\windows\EliteSeriesPay.ico"
    if (-not (Test-Path $IconPath)) {
        throw "Application icon not found: $IconPath"
    }

    $JPackageArgs = @(
        "--type", "msi",
        "--name", $AppName,
        "--app-version", $AppVersion,
        "--vendor", $Vendor,
        "--description", "Local application for managing participants, payments and budgets",
        "--input", $InputDir,
        "--main-jar", $MainJar,
        "--main-class", $SpringBootMainClass,
        "--dest", $OutputDir,
        "--icon", $IconPath,
        "--install-dir", $InstallDirName,
        "--win-menu",
        "--win-menu-group", $AppName,
        "--win-upgrade-uuid", $WinUpgradeUuid,
        "--java-options", "-Deliteseriespay.packaged=true",
        "--java-options", "-Djava.awt.headless=false"
    )

    if (-not $SkipDesktopShortcut) {
        $JPackageArgs += "--win-shortcut"
    }

    if (Test-Path $OutputDir) {
        Remove-Item $OutputDir -Recurse -Force
    }
    New-Item -ItemType Directory -Path $OutputDir | Out-Null

    Write-Host "Creating Windows MSI installer..."
    & jpackage @JPackageArgs
    if ($LASTEXITCODE -ne 0) {
        throw "jpackage failed with exit code $LASTEXITCODE"
    }

    $MsiFileName = "$AppName-$AppVersion.msi"
    $MsiPath = Join-Path $OutputDir $MsiFileName
    if (-not (Test-Path $MsiPath)) {
        throw "MSI installer was not created: $MsiPath"
    }

    Write-Host ""
    Write-Host "MSI installer created:"
    Write-Host "  $MsiPath"
    Write-Host ""
    Write-Host "MSI app version: $AppVersion"
    Write-Host "MSI upgrade UUID: $WinUpgradeUuid"
    Write-Host ""
    Write-Host "User data directory:"
    Write-Host "  %LOCALAPPDATA%\EliteSeriesPay\"
    Write-Host "    data\eliteseriespay.db"
    Write-Host "    backups\"
    Write-Host "    logs\"
    Write-Host ""
    Write-Host "Installed application directory (requires administrator):"
    Write-Host "  C:\Program Files\$InstallDirName\"
    Write-Host ""
    Write-Host "User data is never removed during upgrade or uninstall."
    Write-Host ""
    Write-Host "Verify installed launcher options in:"
    Write-Host "  C:\Program Files\$InstallDirName\app\$AppName.cfg"
}
finally {
    Pop-Location
}
