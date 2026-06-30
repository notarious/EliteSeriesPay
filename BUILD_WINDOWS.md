# Building the Windows MSI Installer

This guide describes how to build a Windows MSI installer for EliteSeriesPay using `jpackage`.

The installer:

- bundles a Java 21 runtime (end users do not need Java installed)
- installs application files to `C:\Program Files\EliteSeriesPay\` (administrator privileges may be required)
- creates a Start Menu shortcut
- optionally creates a Desktop shortcut
- stores user data separately under `%LOCALAPPDATA%\EliteSeriesPay\`
- never deletes user data during upgrade or uninstall

User data layout:

```
%LOCALAPPDATA%\EliteSeriesPay\
  data\
    eliteseriespay.db
  backups\
  logs\
    startup.log
    application.log
  eliteseriespay.instance.lock
```

The SQLite database is never stored inside the application installation directory. It always resolves to:

```
%LOCALAPPDATA%\EliteSeriesPay\data\eliteseriespay.db
```

On first startup after upgrading from older versions, if a database exists at the legacy location `%LOCALAPPDATA%\EliteSeriesPay\eliteseriespay.db`, the application moves it into the `data\` subdirectory automatically.

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

Before building a **release** MSI, increase the version in `pom.xml` (for example from `0.0.1-SNAPSHOT` to `0.0.2-SNAPSHOT`). The build script strips the `-SNAPSHOT` suffix and passes the result to `jpackage` as `--app-version`.

Each new release MSI must have a **higher** app version than the installed one so Windows treats it as an upgrade.

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
2. verifies the Spring Boot executable JAR (`Main-Class: org.springframework.boot.loader.launch.JarLauncher`)
3. copies only the executable JAR into `target\jpackage-input`
4. invokes `jpackage --type msi` to create an MSI with a bundled runtime
5. embeds the application icon from `installer\windows\EliteSeriesPay.ico` via `--icon` (desktop shortcut, Start Menu, executable, and taskbar)
6. passes `-Deliteseriespay.packaged=true` and `-Djava.awt.headless=false` for packaged desktop behavior (system tray, browser launch)
7. uses a fixed `--win-upgrade-uuid` so a newer MSI replaces only application files in `C:\Program Files\EliteSeriesPay\`

## Upgrading an existing installation

Newer MSI installers use a stable Windows upgrade UUID. When you run a newer `EliteSeriesPay-<version>.msi`, Windows should offer to upgrade the existing installation instead of reporting that another version is already installed.

User data is **not** stored in the install directory and is **not** removed during upgrade or uninstall:

```
%LOCALAPPDATA%\EliteSeriesPay\
  data\
    eliteseriespay.db
  backups\
  logs\
```

Before Flyway migrations run, the application automatically creates a timestamped backup of the existing database in `%LOCALAPPDATA%\EliteSeriesPay\backups\`.

The instance lock file is temporary and is recreated on the next launch.

### Update procedure

1. Exit EliteSeriesPay from the tray menu: **Выход**
2. Run the new MSI (`EliteSeriesPay-<version>.msi`) and approve the UAC prompt if shown
3. Follow the installer prompts to upgrade
4. Launch EliteSeriesPay from the Start Menu

If the application is still running, the installer may fail to replace files in `C:\Program Files\EliteSeriesPay\`.

### One-time note for older builds

MSI packages built before the fixed upgrade UUID was introduced may still require a one-time manual uninstall. After that, future updates should install over the existing version.

## Resulting MSI

The installer is written to:

```
target\dist\EliteSeriesPay-<version>.msi
```

Example:

```
target\dist\EliteSeriesPay-0.0.2.msi
```

After installation:

- application files: `C:\Program Files\EliteSeriesPay\`
- launcher config: `C:\Program Files\EliteSeriesPay\app\EliteSeriesPay.cfg`
- Start Menu shortcut: `EliteSeriesPay`
- Desktop shortcut: created by default (disable with `-SkipDesktopShortcut`)
- application icon: custom icon from `installer\windows\EliteSeriesPay.ico` (not the default Java icon)
- user data: `%LOCALAPPDATA%\EliteSeriesPay\`

The installed `EliteSeriesPay.cfg` must contain:

```
java-options=-Deliteseriespay.packaged=true
java-options=-Djava.awt.headless=false
```

On first launch, inspect `%LOCALAPPDATA%\EliteSeriesPay\logs\startup.log` to verify resolved paths.

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

## Troubleshooting with app-image

If MSI creation fails (for example, WiX is missing or misconfigured), you can verify the launcher separately by building an **app-image** instead of an MSI.

Temporarily change `--type msi` to `--type app-image` in `packaging\windows\build-windows-msi.ps1` and remove the MSI-only options (`--install-dir`, `--win-menu`, `--win-menu-group`, `--win-shortcut`). Keep the launcher options:

- `--main-jar`
- `--main-class`
- `--java-options -Deliteseriespay.packaged=true`
- `--java-options -Djava.awt.headless=false`

The unpacked executable is written to:

```
target\dist\EliteSeriesPay\EliteSeriesPay.exe
```

Use this flow to test startup, tray, single-instance behavior, and backups before restoring MSI packaging.
