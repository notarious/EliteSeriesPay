package com.eliteseriespay.config;

import java.nio.file.Path;
import java.util.Locale;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

public final class ApplicationDataDirectory {

    static final String DATA_DIR_PROPERTY = "eliteseriespay.data-dir";
    static final String PACKAGED_PROPERTY = "eliteseriespay.packaged";
    static final String APP_FOLDER_NAME = "EliteSeriesPay";
    static final String DATABASE_FILE_NAME = "eliteseriespay.db";
    static final String BACKUPS_FOLDER_NAME = "backups";

    private ApplicationDataDirectory() {
    }

    public static boolean shouldOverrideDefaultPaths(ConfigurableEnvironment environment) {
        if (StringUtils.hasText(environment.getProperty(DATA_DIR_PROPERTY))) {
            return true;
        }
        return Boolean.TRUE.equals(environment.getProperty(PACKAGED_PROPERTY, Boolean.class))
                && isWindows();
    }

    public static Path resolve(ConfigurableEnvironment environment) {
        return resolve(
                environment.getProperty(DATA_DIR_PROPERTY),
                Boolean.TRUE.equals(environment.getProperty(PACKAGED_PROPERTY, Boolean.class)),
                System.getenv("LOCALAPPDATA"),
                Path.of(System.getProperty("user.dir")));
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

    public static String toJdbcSqliteFileUrl(Path databaseFile) {
        String normalizedPath =
                databaseFile.toAbsolutePath().normalize().toString().replace('\\', '/');
        return "jdbc:sqlite:file:" + normalizedPath + "?busy_timeout=5000";
    }

    static boolean isWindows() {
        String osName = System.getProperty("os.name", "");
        return osName.toLowerCase(Locale.ROOT).startsWith("windows");
    }
}
