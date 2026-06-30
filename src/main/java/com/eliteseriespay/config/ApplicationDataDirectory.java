package com.eliteseriespay.config;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

public final class ApplicationDataDirectory {

    private static final Logger log = LoggerFactory.getLogger(ApplicationDataDirectory.class);

    public static final String DATA_DIR_PROPERTY = "eliteseriespay.data-dir";
    public static final String PACKAGED_PROPERTY = "eliteseriespay.packaged";
    static final String APP_FOLDER_NAME = "EliteSeriesPay";
    static final String DATA_SUBFOLDER_NAME = "data";
    static final String DATABASE_FILE_NAME = "eliteseriespay.db";
    static final String BACKUPS_FOLDER_NAME = "backups";
    static final String LOGS_FOLDER_NAME = "logs";

    private ApplicationDataDirectory() {
    }

    public static void logPackagedStartupPreSpring() {
        PackagedStartupDiagnostics.logPreSpring();
    }

    public static void ensurePackagedModeInitialized() {
        if (isPackagedPreSpring() && isWindows()) {
            System.setProperty(PACKAGED_PROPERTY, "true");
        }
    }

    public static boolean shouldOverrideDefaultPaths(ConfigurableEnvironment environment) {
        if (StringUtils.hasText(environment.getProperty(DATA_DIR_PROPERTY))) {
            return true;
        }
        return isPackaged(environment) && isWindows();
    }

    public static boolean isPackaged(ConfigurableEnvironment environment) {
        if (isPackagedPropertyEnabled(environment.getProperty(PACKAGED_PROPERTY))) {
            return true;
        }
        if (isPackagedPropertyEnabled(System.getProperty(PACKAGED_PROPERTY))) {
            return true;
        }
        return isJPackageApplicationLayout();
    }

    public static boolean isPackagedPreSpring() {
        if (isPackagedPropertyEnabled(System.getProperty(PACKAGED_PROPERTY))) {
            return true;
        }
        return isJPackageApplicationLayout();
    }

    public static Path resolve(ConfigurableEnvironment environment) {
        return resolve(
                environment.getProperty(DATA_DIR_PROPERTY),
                isPackaged(environment),
                localAppDataDirectory(environment),
                Path.of(System.getProperty("user.dir", ".")));
    }

    public static Path resolvePreSpring() {
        return resolve(
                null,
                isPackagedPreSpring(),
                System.getenv("LOCALAPPDATA"),
                Path.of(System.getProperty("user.dir", ".")));
    }

    static Path resolve(String dataDir, boolean packaged, String localAppData, Path workingDirectory) {
        if (StringUtils.hasText(dataDir)) {
            return Path.of(dataDir.trim());
        }
        if (packaged) {
            if (!StringUtils.hasText(localAppData)) {
                throw new IllegalStateException("LOCALAPPDATA environment variable is not set");
            }
            return Path.of(localAppData, APP_FOLDER_NAME);
        }
        return workingDirectory;
    }

    public static Path databaseStorageDirectory(Path dataDirectory) {
        return dataDirectory.resolve(DATA_SUBFOLDER_NAME);
    }

    public static Path databasePath(Path dataDirectory) {
        return databaseStorageDirectory(dataDirectory).resolve(DATABASE_FILE_NAME);
    }

    public static Path legacyDatabasePath(Path dataDirectory) {
        return dataDirectory.resolve(DATABASE_FILE_NAME);
    }

    public static Path backupsDirectory(Path dataDirectory) {
        return dataDirectory.resolve(BACKUPS_FOLDER_NAME);
    }

    public static Path logsDirectory(Path dataDirectory) {
        return dataDirectory.resolve(LOGS_FOLDER_NAME);
    }

    public static Path applicationDirectory() {
        Path codeSourcePath = detectCodeSourcePath();
        if (codeSourcePath == null) {
            return null;
        }

        Path normalizedPath = codeSourcePath.toAbsolutePath().normalize();
        Path appDirectory = normalizedPath.getParent();
        if (appDirectory == null || !"app".equalsIgnoreCase(appDirectory.getFileName().toString())) {
            return null;
        }

        Path installRoot = appDirectory.getParent();
        return installRoot != null ? installRoot.normalize() : null;
    }

    public static void assertDatabaseNotInsideApplicationDirectory(Path databaseFile) {
        assertDatabaseNotInsideApplicationDirectory(databaseFile, applicationDirectory());
    }

    static void assertDatabaseNotInsideApplicationDirectory(Path databaseFile, Path applicationDirectory) {
        if (applicationDirectory == null) {
            return;
        }

        Path normalizedDatabase = databaseFile.toAbsolutePath().normalize();
        Path normalizedApplication = applicationDirectory.toAbsolutePath().normalize();
        if (normalizedDatabase.startsWith(normalizedApplication)) {
            throw new IllegalStateException(
                    "Database must not be stored inside the application installation directory. "
                            + "Application directory: "
                            + normalizedApplication
                            + ", database path: "
                            + normalizedDatabase);
        }
    }

    public static DatabaseLocationMigrationResult migrateLegacyDatabaseIfNeeded(Path dataDirectory) {
        Path legacyPath = legacyDatabasePath(dataDirectory);
        Path targetPath = databasePath(dataDirectory);

        if (!Files.exists(legacyPath)) {
            log.info(
                    "Legacy database migration not required: no database at {}",
                    legacyPath.toAbsolutePath().normalize());
            return DatabaseLocationMigrationResult.notNeeded(legacyPath, targetPath);
        }

        if (Files.exists(targetPath)) {
            log.info(
                    "Legacy database migration skipped: destination already exists at {} (legacy file remains at {})",
                    targetPath.toAbsolutePath().normalize(),
                    legacyPath.toAbsolutePath().normalize());
            return DatabaseLocationMigrationResult.skippedDestinationExists(legacyPath, targetPath);
        }

        try {
            Files.createDirectories(databaseStorageDirectory(dataDirectory));
            Files.move(legacyPath, targetPath);
            log.info(
                    "Legacy database migrated from {} to {}",
                    legacyPath.toAbsolutePath().normalize(),
                    targetPath.toAbsolutePath().normalize());
            return DatabaseLocationMigrationResult.moved(legacyPath, targetPath);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to migrate legacy database from "
                            + legacyPath.toAbsolutePath().normalize()
                            + " to "
                            + targetPath.toAbsolutePath().normalize(),
                    exception);
        }
    }

    public static String toJdbcSqliteFileUrl(Path databaseFile) {
        String fileUri = databaseFile.toAbsolutePath().normalize().toUri().toString();
        return "jdbc:sqlite:" + fileUri + "?busy_timeout=5000";
    }

    public static boolean isWindows() {
        String osName = System.getProperty("os.name", "");
        return osName.toLowerCase(Locale.ROOT).startsWith("windows");
    }

    public static boolean isJPackageApplicationLayout() {
        return isJPackageApplicationLayout(detectCodeSourcePath());
    }

    static boolean isJPackageApplicationLayout(Path codeSourcePath) {
        if (codeSourcePath == null) {
            return false;
        }

        Path normalizedPath = codeSourcePath.toAbsolutePath().normalize();
        String fileName = normalizedPath.getFileName().toString();
        if (!fileName.endsWith(".jar")) {
            return false;
        }

        Path parent = normalizedPath.getParent();
        return parent != null && "app".equalsIgnoreCase(parent.getFileName().toString());
    }

    private static Path detectCodeSourcePath() {
        try {
            var protectionDomain = ApplicationDataDirectory.class.getProtectionDomain();
            if (protectionDomain == null || protectionDomain.getCodeSource() == null) {
                return null;
            }

            return toJarPath(protectionDomain.getCodeSource().getLocation().toURI());
        } catch (Exception exception) {
            return null;
        }
    }

    static Path toJarPathForTest(URI location) {
        return toJarPath(location);
    }

    private static Path toJarPath(URI location) {
        if ("file".equalsIgnoreCase(location.getScheme())) {
            return Path.of(location).normalize();
        }

        if ("jar".equalsIgnoreCase(location.getScheme())) {
            String schemeSpecificPart = location.getRawSchemeSpecificPart();
            int separatorIndex = schemeSpecificPart.indexOf('!');
            String jarFileUri = separatorIndex >= 0 ? schemeSpecificPart.substring(0, separatorIndex) : schemeSpecificPart;
            if (jarFileUri.startsWith("file:")) {
                return Path.of(URI.create(jarFileUri)).normalize();
            }
        }

        return null;
    }

    private static String localAppDataDirectory(ConfigurableEnvironment environment) {
        String fromEnvironment = environment.getProperty("LOCALAPPDATA");
        if (StringUtils.hasText(fromEnvironment)) {
            return fromEnvironment;
        }
        return System.getenv("LOCALAPPDATA");
    }

    private static boolean isPackagedPropertyEnabled(String value) {
        return Boolean.parseBoolean(value);
    }
}
