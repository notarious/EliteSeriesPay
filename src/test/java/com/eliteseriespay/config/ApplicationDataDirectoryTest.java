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
        String normalizedPath = databaseFile.toAbsolutePath().normalize().toString().replace('\\', '/');

        assertThat(ApplicationDataDirectory.toJdbcSqliteFileUrl(databaseFile))
                .isEqualTo("jdbc:sqlite:file:" + normalizedPath + "?busy_timeout=5000");
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
