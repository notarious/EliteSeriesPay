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

    $JPackageArgs = @(
        "--type", "app-image",
        "--name", $AppName,
        "--app-version", $AppVersion,
        "--vendor", $Vendor,
        "--description", "Local application for managing participants, payments and budgets",
        "--input", $InputDir,
        "--main-jar", $MainJar,
        "--main-class", $SpringBootMainClass,
        "--dest", $OutputDir,
        "--java-options", "-Deliteseriespay.packaged=true",
        "--java-options", "-Djava.awt.headless=false"
    )

    # MSI-only options (--install-dir, --win-menu, --win-shortcut) omitted for app-image

    if (Test-Path $OutputDir) {
        Remove-Item $OutputDir -Recurse -Force
    }
    New-Item -ItemType Directory -Path $OutputDir | Out-Null

    Write-Host "Creating Windows app-image..."
    & jpackage @JPackageArgs
    if ($LASTEXITCODE -ne 0) {
        throw "jpackage failed with exit code $LASTEXITCODE"
    }

    $AppImageExe = Join-Path $OutputDir "$AppName\$AppName.exe"
    if (-not (Test-Path $AppImageExe)) {
        throw "App-image executable was not created: $AppImageExe"
    }

    Write-Host ""
    Write-Host "App-image created:"
    Write-Host "  $AppImageExe"
    Write-Host ""
    Write-Host "User data directory:"
    Write-Host "  %LOCALAPPDATA%\EliteSeriesPay"
}
finally {
    Pop-Location
}
