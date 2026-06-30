package com.eliteseriespay.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

class ApplicationDataDirectoryTest {

    @Test
    void resolve_usesExplicitDataDirWhenConfigured() {
        Path dataDir = Path.of("/custom/data");

        assertThat(ApplicationDataDirectory.resolve(dataDir.toString(), false, null, Path.of("/work")))
                .isEqualTo(dataDir);
    }

    @Test
    void resolve_usesLocalAppDataForPackagedWindowsInstall() {
        Path dataDir = ApplicationDataDirectory.resolve(
                null,
                true,
                "C:\\Users\\test\\AppData\\Local",
                Path.of("C:\\work"));

        assertThat(dataDir).isEqualTo(Path.of("C:\\Users\\test\\AppData\\Local", "EliteSeriesPay"));
    }

    @Test
    void resolve_usesWorkingDirectoryForDevelopment() {
        Path workingDirectory = Path.of("/work");

        assertThat(ApplicationDataDirectory.resolve(null, false, null, workingDirectory))
                .isEqualTo(workingDirectory);
    }

    @Test
    void resolve_rejectsPackagedInstallWithoutLocalAppData() {
        assertThatThrownBy(() -> ApplicationDataDirectory.resolve(null, true, null, Path.of("/work")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("LOCALAPPDATA environment variable is not set");
    }

    @Test
    void shouldOverrideDefaultPaths_whenExplicitDataDirConfigured() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty(ApplicationDataDirectory.DATA_DIR_PROPERTY, "C:\\data");

        assertThat(ApplicationDataDirectory.shouldOverrideDefaultPaths(environment)).isTrue();
    }

    @Test
    void shouldOverrideDefaultPaths_returnsFalseForDevelopmentDefaults() {
        MockEnvironment environment = new MockEnvironment();

        assertThat(ApplicationDataDirectory.shouldOverrideDefaultPaths(environment)).isFalse();
    }

    @Test
    void toJdbcSqliteFileUrl_usesAbsolutePath() {
        Path databaseFile = Path.of("data-dir", "data", "eliteseriespay.db");
        String expected = "jdbc:sqlite:" + databaseFile.toAbsolutePath().normalize().toUri() + "?busy_timeout=5000";

        assertThat(ApplicationDataDirectory.toJdbcSqliteFileUrl(databaseFile)).isEqualTo(expected);
    }

    @Test
    void resolve_readsLocalAppDataFromEnvironment() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("LOCALAPPDATA", "C:\\Users\\test\\AppData\\Local")
                .withProperty(ApplicationDataDirectory.PACKAGED_PROPERTY, "true");

        assertThat(ApplicationDataDirectory.resolve(environment))
                .isEqualTo(Path.of("C:\\Users\\test\\AppData\\Local", "EliteSeriesPay"));
    }

    @Test
    void toJdbcSqliteFileUrl_canOpenDatabaseFile() throws Exception {
        Path databaseFile = Files.createTempDirectory("esp-sqlite-url-test")
                .resolve("data")
                .resolve("eliteseriespay.db");
        Files.createDirectories(databaseFile.getParent());

        try (var connection = java.sql.DriverManager.getConnection(
                ApplicationDataDirectory.toJdbcSqliteFileUrl(databaseFile))) {
            assertThat(connection).isNotNull();
            assertThat(Files.exists(databaseFile)).isTrue();
        } finally {
            Files.deleteIfExists(databaseFile);
        }
    }

    @Test
    void toJdbcSqliteFileUrl_avoidsBrokenWindowsDriveLetterUriScheme() {
        String jdbcUrl = ApplicationDataDirectory.toJdbcSqliteFileUrl(
                Path.of("/tmp/EliteSeriesPay/data/eliteseriespay.db"));

        assertThat(jdbcUrl).startsWith("jdbc:sqlite:file:");
        assertThat(jdbcUrl).doesNotContain("file:C:");
        assertThat(jdbcUrl).doesNotContain("file:D:");
    }

    @Test
    void isPackaged_readsSystemPropertyWhenEnvironmentPropertyMissing() {
        String originalValue = System.getProperty(ApplicationDataDirectory.PACKAGED_PROPERTY);
        try {
            System.setProperty(ApplicationDataDirectory.PACKAGED_PROPERTY, "true");

            assertThat(ApplicationDataDirectory.isPackaged(new MockEnvironment())).isTrue();
        } finally {
            restoreSystemProperty(ApplicationDataDirectory.PACKAGED_PROPERTY, originalValue);
        }
    }

    private static void restoreSystemProperty(String key, String originalValue) {
        if (originalValue == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, originalValue);
        }
    }

    @Test
    void databasePathAndBackupsDirectory_areUnderDataDirectory() {
        Path dataDirectory = Path.of("C:\\Users\\test\\AppData\\Local\\EliteSeriesPay");

        assertThat(ApplicationDataDirectory.databaseStorageDirectory(dataDirectory))
                .isEqualTo(dataDirectory.resolve("data"));
        assertThat(ApplicationDataDirectory.databasePath(dataDirectory))
                .isEqualTo(dataDirectory.resolve("data").resolve("eliteseriespay.db"));
        assertThat(ApplicationDataDirectory.legacyDatabasePath(dataDirectory))
                .isEqualTo(dataDirectory.resolve("eliteseriespay.db"));
        assertThat(ApplicationDataDirectory.backupsDirectory(dataDirectory))
                .isEqualTo(dataDirectory.resolve("backups"));
        assertThat(ApplicationDataDirectory.logsDirectory(dataDirectory))
                .isEqualTo(dataDirectory.resolve("logs"));
    }

    @Test
    void applicationDirectory_resolvesFromJPackageLayout() {
        Path jarPath = Path.of("C:\\Program Files\\EliteSeriesPay\\app\\eliteseriespay.jar");

        assertThat(ApplicationDataDirectory.isJPackageApplicationLayout(jarPath)).isTrue();
    }

    @Test
    void applicationDirectory_resolvesInstallRootFromJarPath() throws Exception {
        URI codeSource = URI.create(
                "jar:file:/C:/Program%20Files/EliteSeriesPay/app/eliteseriespay-0.0.1-SNAPSHOT.jar!/BOOT-INF/classes!/");

        Path jarPath = ApplicationDataDirectory.toJarPathForTest(codeSource);
        assertThat(jarPath).isNotNull();
        assertThat(jarPath.getParent().getFileName().toString()).isEqualTo("app");
        assertThat(jarPath.getParent().getParent()).isEqualTo(Path.of("C:\\Program Files\\EliteSeriesPay"));
    }

    @Test
    void isJPackageApplicationLayout_ignoresDevelopmentJarLocation() {
        Path jarPath = Path.of("D:\\project\\target\\eliteseriespay.jar");

        assertThat(ApplicationDataDirectory.isJPackageApplicationLayout(jarPath)).isFalse();
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void resolvePreSpring_usesLocalAppDataWhenPackagedPropertySet() {
        String originalValue = System.getProperty(ApplicationDataDirectory.PACKAGED_PROPERTY);
        try {
            System.setProperty(ApplicationDataDirectory.PACKAGED_PROPERTY, "true");

            Path dataDirectory = ApplicationDataDirectory.resolvePreSpring();

            assertThat(dataDirectory.getFileName().toString()).isEqualTo("EliteSeriesPay");
            assertThat(dataDirectory.getParent()).isEqualTo(Path.of(System.getenv("LOCALAPPDATA")));
        } finally {
            restoreSystemProperty(ApplicationDataDirectory.PACKAGED_PROPERTY, originalValue);
        }
    }

    @Test
    void ensurePackagedModeInitialized_setsPackagedPropertyOnWindowsWhenPackaged() {
        String originalValue = System.getProperty(ApplicationDataDirectory.PACKAGED_PROPERTY);
        try {
            System.setProperty(ApplicationDataDirectory.PACKAGED_PROPERTY, "true");
            ApplicationDataDirectory.ensurePackagedModeInitialized();
            assertThat(System.getProperty(ApplicationDataDirectory.PACKAGED_PROPERTY)).isEqualTo("true");
        } finally {
            restoreSystemProperty(ApplicationDataDirectory.PACKAGED_PROPERTY, originalValue);
        }
    }

    @Test
    void migrateLegacyDatabaseIfNeeded_movesDatabaseFromLegacyLocation(@TempDir Path tempDir) throws Exception {
        Path legacyDatabase = ApplicationDataDirectory.legacyDatabasePath(tempDir);
        Files.createDirectories(tempDir);
        Files.writeString(legacyDatabase, "legacy-data");

        DatabaseLocationMigrationResult result = ApplicationDataDirectory.migrateLegacyDatabaseIfNeeded(tempDir);

        assertThat(result.status()).isEqualTo(DatabaseLocationMigrationResult.Status.MOVED);
        assertThat(Files.exists(ApplicationDataDirectory.databasePath(tempDir))).isTrue();
        assertThat(Files.exists(legacyDatabase)).isFalse();
        assertThat(Files.readString(ApplicationDataDirectory.databasePath(tempDir))).isEqualTo("legacy-data");
    }

    @Test
    void migrateLegacyDatabaseIfNeeded_skipsWhenDestinationExists(@TempDir Path tempDir) throws Exception {
        Path legacyDatabase = ApplicationDataDirectory.legacyDatabasePath(tempDir);
        Path targetDatabase = ApplicationDataDirectory.databasePath(tempDir);
        Files.createDirectories(targetDatabase.getParent());
        Files.writeString(legacyDatabase, "legacy-data");
        Files.writeString(targetDatabase, "existing-data");

        DatabaseLocationMigrationResult result = ApplicationDataDirectory.migrateLegacyDatabaseIfNeeded(tempDir);

        assertThat(result.status()).isEqualTo(DatabaseLocationMigrationResult.Status.SKIPPED_DESTINATION_EXISTS);
        assertThat(Files.readString(legacyDatabase)).isEqualTo("legacy-data");
        assertThat(Files.readString(targetDatabase)).isEqualTo("existing-data");
    }

    @Test
    void migrateLegacyDatabaseIfNeeded_notNeededWhenLegacyMissing(@TempDir Path tempDir) {
        DatabaseLocationMigrationResult result = ApplicationDataDirectory.migrateLegacyDatabaseIfNeeded(tempDir);

        assertThat(result.status()).isEqualTo(DatabaseLocationMigrationResult.Status.NOT_NEEDED);
    }

    @Test
    void assertDatabaseNotInsideApplicationDirectory_rejectsDatabaseInsideInstallDirectory() {
        Path installRoot = Path.of("C:\\Program Files\\EliteSeriesPay");
        Path databaseInsideInstall = installRoot.resolve("data").resolve("eliteseriespay.db");

        assertThatThrownBy(() -> ApplicationDataDirectory.assertDatabaseNotInsideApplicationDirectory(
                        databaseInsideInstall, installRoot))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Database must not be stored inside the application installation directory");
    }

    @Test
    void assertDatabaseNotInsideApplicationDirectory_allowsDatabaseOutsideInstallDirectory(@TempDir Path tempDir) {
        Path installRoot = Path.of("C:\\Program Files\\EliteSeriesPay");
        Path databasePath = tempDir.resolve("data").resolve("eliteseriespay.db");

        ApplicationDataDirectory.assertDatabaseNotInsideApplicationDirectory(databasePath, installRoot);
    }
}
