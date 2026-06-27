package com.eliteseriespay.config;

import java.net.URI;
import java.nio.file.Path;
import java.util.Locale;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

public final class ApplicationDataDirectory {

    public static final String DATA_DIR_PROPERTY = "eliteseriespay.data-dir";
    public static final String PACKAGED_PROPERTY = "eliteseriespay.packaged";
    static final String APP_FOLDER_NAME = "EliteSeriesPay";
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

    public static Path databasePath(Path dataDirectory) {
        return dataDirectory.resolve(DATABASE_FILE_NAME);
    }

    public static Path backupsDirectory(Path dataDirectory) {
        return dataDirectory.resolve(BACKUPS_FOLDER_NAME);
    }

    public static Path logsDirectory(Path dataDirectory) {
        return dataDirectory.resolve(LOGS_FOLDER_NAME);
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
