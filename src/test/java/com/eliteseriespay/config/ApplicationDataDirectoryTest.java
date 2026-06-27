package com.eliteseriespay.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
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
        Path databaseFile = Path.of("data-dir", "eliteseriespay.db");
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
        Path databaseFile = java.nio.file.Files.createTempDirectory("esp-sqlite-url-test")
                .resolve("eliteseriespay.db");

        try (var connection = java.sql.DriverManager.getConnection(
                ApplicationDataDirectory.toJdbcSqliteFileUrl(databaseFile))) {
            assertThat(connection).isNotNull();
            assertThat(java.nio.file.Files.exists(databaseFile)).isTrue();
        } finally {
            java.nio.file.Files.deleteIfExists(databaseFile);
        }
    }

    @Test
    void toJdbcSqliteFileUrl_avoidsBrokenWindowsDriveLetterUriScheme() {
        String jdbcUrl = ApplicationDataDirectory.toJdbcSqliteFileUrl(
                Path.of("/tmp/EliteSeriesPay/eliteseriespay.db"));

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

        assertThat(ApplicationDataDirectory.databasePath(dataDirectory))
                .isEqualTo(dataDirectory.resolve("eliteseriespay.db"));
        assertThat(ApplicationDataDirectory.backupsDirectory(dataDirectory))
                .isEqualTo(dataDirectory.resolve("backups"));
    }
}
