# Building the Windows MSI Installer

This guide describes how to build a Windows MSI installer for EliteSeriesPay using `jpackage`.

The installer:

- bundles a Java 21 runtime (end users do not need Java installed)
- installs the application to `Program Files`
- creates a Start Menu shortcut
- optionally creates a Desktop shortcut
- stores user data in `%LOCALAPPDATA%\EliteSeriesPay`

User data layout:

```
%LOCALAPPDATA%\EliteSeriesPay\
  eliteseriespay.db
  backups\
```

The SQLite database is created automatically on first launch.

## Prerequisites

Build the MSI on a Windows machine.

1. **JDK 21** with `jpackage` available on `PATH`
2. **Maven**
3. **WiX Toolset 3.x** (required by `jpackage` for MSI creation)
   - download from [WiX Toolset releases](https://github.com/wixtoolset/wix3/releases)
   - add the WiX `bin` directory to `PATH`

Verify tools:

```bat
java -version
mvn -version
jpackage --version
candle -?
```

## Build command

From the project root:

```bat
packaging\windows\build-windows-msi.bat
```

PowerShell:

```powershell
.\packaging\windows\build-windows-msi.ps1
```

Useful options:

```powershell
# Skip tests for a faster packaging build
.\packaging\windows\build-windows-msi.ps1 -SkipTests

# Build without a Desktop shortcut
.\packaging\windows\build-windows-msi.ps1 -SkipDesktopShortcut
```

The script:

1. runs `mvn package`
2. invokes `jpackage` to create an MSI with a bundled runtime
3. passes `-Deliteseriespay.packaged=true` so the app stores data under `%LOCALAPPDATA%\EliteSeriesPay`

## Resulting MSI

The installer is written to:

```
target\dist\EliteSeriesPay-<version>.msi
```

Example:

```
target\dist\EliteSeriesPay-0.0.1.msi
```

After installation:

- application files: `C:\Program Files\EliteSeriesPay\`
- Start Menu shortcut: `EliteSeriesPay`
- Desktop shortcut: created by default (disable with `-SkipDesktopShortcut`)
- user data: `%LOCALAPPDATA%\EliteSeriesPay\`

Launch the installed application and open:

```
http://localhost:8080
```

## Development vs packaged installs

When running from Maven (`mvn spring-boot:run`), the database and backups remain in the project working directory:

```
./eliteseriespay.db
./backups/
```

The `%LOCALAPPDATA%` location is used only by the packaged Windows installer.
