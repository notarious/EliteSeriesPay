param(
    [switch]$SkipTests,
    [switch]$SkipDesktopShortcut
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$AppName = "EliteSeriesPay"
$Vendor = "EliteSeriesPay"
$InstallDirName = "EliteSeriesPay"

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
    $MainJar = "$ProjectArtifactId-$ProjectVersion.jar"
    $InputDir = Join-Path $ProjectRoot "target"
    $OutputDir = Join-Path $ProjectRoot "target\dist"
    $MainJarPath = Join-Path $InputDir $MainJar

    if (-not (Test-Path $MainJarPath)) {
        throw "Application JAR not found: $MainJarPath"
    }

    if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) {
        throw "jpackage was not found. Install JDK 21 and ensure jpackage is on PATH."
    }

    $JPackageArgs = @(
        "--type", "msi",
        "--name", $AppName,
        "--app-version", $AppVersion,
        "--vendor", $Vendor,
        "--description", "Local application for managing participants, payments and budgets",
        "--input", $InputDir,
        "--main-jar", $MainJar,
        "--dest", $OutputDir,
        "--install-dir", $InstallDirName,
        "--win-menu",
        "--win-menu-group", $AppName,
        "--java-options", "-Deliteseriespay.packaged=true"
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

    $MsiFile = Get-ChildItem -Path $OutputDir -Filter "*.msi" | Select-Object -First 1
    if ($null -eq $MsiFile) {
        throw "MSI file was not created in $OutputDir"
    }

    Write-Host ""
    Write-Host "MSI installer created:"
    Write-Host "  $($MsiFile.FullName)"
    Write-Host ""
    Write-Host "User data directory:"
    Write-Host "  %LOCALAPPDATA%\EliteSeriesPay"
}
finally {
    Pop-Location
}
