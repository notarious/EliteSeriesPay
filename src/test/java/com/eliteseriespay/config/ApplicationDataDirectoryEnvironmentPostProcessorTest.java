package com.eliteseriespay.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.eliteseriespay.backup.PreFlywayDatabaseBackup;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

class ApplicationDataDirectoryEnvironmentPostProcessorTest {

    @TempDir
    Path tempDir;

    @Test
    void postProcessEnvironment_configuresDatabaseAndBackupPaths() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty(ApplicationDataDirectory.DATA_DIR_PROPERTY, tempDir.toString());

        new ApplicationDataDirectoryEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new SpringApplication());

        Path expectedDatabase = tempDir.resolve("data").resolve("eliteseriespay.db");
        Path expectedBackups = tempDir.resolve("backups");

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo(ApplicationDataDirectory.toJdbcSqliteFileUrl(expectedDatabase));
        assertThat(environment.getProperty("eliteseriespay.database-backup.database-path"))
                .isEqualTo(expectedDatabase.toString());
        assertThat(environment.getProperty("eliteseriespay.database-backup.backup-directory"))
                .isEqualTo(expectedBackups.toString());
        assertThat(environment.getProperty("logging.file.name"))
                .isEqualTo(tempDir.resolve("logs").resolve("application.log").toString());
        assertThat(Files.isDirectory(tempDir.resolve("data"))).isTrue();
        assertThat(Files.isDirectory(expectedBackups)).isTrue();
        assertThat(Files.isDirectory(tempDir.resolve("logs"))).isTrue();
    }

    @Test
    void postProcessEnvironment_configuresPackagedWindowsPaths(@TempDir Path localAppDataRoot) {
        String originalOsName = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Windows 11");

            MockEnvironment environment = new MockEnvironment()
                    .withProperty(ApplicationDataDirectory.PACKAGED_PROPERTY, "true")
                    .withProperty("LOCALAPPDATA", localAppDataRoot.toString());

            new ApplicationDataDirectoryEnvironmentPostProcessor()
                    .postProcessEnvironment(environment, new SpringApplication());

            Path expectedDataDirectory = localAppDataRoot.resolve("EliteSeriesPay");
            Path expectedDatabase = expectedDataDirectory.resolve("data").resolve("eliteseriespay.db");
            Path expectedBackups = expectedDataDirectory.resolve("backups");

            assertThat(environment.getProperty("spring.datasource.url"))
                    .isEqualTo(ApplicationDataDirectory.toJdbcSqliteFileUrl(expectedDatabase));
            assertThat(environment.getProperty("logging.file.name"))
                    .isEqualTo(expectedDataDirectory.resolve("logs").resolve("application.log").toString());
            assertThat(Files.isDirectory(expectedDataDirectory.resolve("data"))).isTrue();
            assertThat(Files.isDirectory(expectedBackups)).isTrue();
            assertThat(Files.isDirectory(expectedDataDirectory.resolve("logs"))).isTrue();
        } finally {
            if (originalOsName == null) {
                System.clearProperty("os.name");
            } else {
                System.setProperty("os.name", originalOsName);
            }
        }
    }

    @Test
    void postProcessEnvironment_migratesLegacyDatabase(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("eliteseriespay.db"), "legacy-data");

        MockEnvironment environment = new MockEnvironment()
                .withProperty(ApplicationDataDirectory.DATA_DIR_PROPERTY, tempDir.toString());

        new ApplicationDataDirectoryEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new SpringApplication());

        Path expectedDatabase = tempDir.resolve("data").resolve("eliteseriespay.db");
        assertThat(Files.exists(expectedDatabase)).isTrue();
        assertThat(Files.readString(expectedDatabase)).isEqualTo("legacy-data");
        assertThat(Files.exists(tempDir.resolve("eliteseriespay.db"))).isFalse();
    }

    @Test
    void postProcessEnvironment_createsPreFlywayBackup(@TempDir Path tempDir) throws Exception {
        Path databasePath = tempDir.resolve("data").resolve("eliteseriespay.db");
        Files.createDirectories(databasePath.getParent());
        Files.writeString(databasePath, "sqlite-data");

        MockEnvironment environment = new MockEnvironment()
                .withProperty(ApplicationDataDirectory.DATA_DIR_PROPERTY, tempDir.toString());

        new ApplicationDataDirectoryEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new SpringApplication());

        try (var backupFiles = Files.list(tempDir.resolve("backups"))) {
            assertThat(backupFiles.filter(Files::isRegularFile).count()).isEqualTo(1L);
        }
    }

    @Test
    void postProcessEnvironment_keepsDevelopmentDefaults() {
        MockEnvironment environment = new MockEnvironment();

        new ApplicationDataDirectoryEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.url")).isNull();
        assertThat(environment.getProperty("eliteseriespay.database-backup.database-path")).isNull();
    }

    @Test
    void preFlywayBackup_skipsMissingDatabase(@TempDir Path tempDir) {
        Path databasePath = tempDir.resolve("data").resolve("eliteseriespay.db");
        Path backupDirectory = tempDir.resolve("backups");

        assertThat(PreFlywayDatabaseBackup.backupIfDatabaseExists(databasePath, backupDirectory)).isEmpty();
    }

    @Test
    void preFlywayBackup_createsTimestampedBackup(@TempDir Path tempDir) throws Exception {
        Path databasePath = tempDir.resolve("data").resolve("eliteseriespay.db");
        Path backupDirectory = tempDir.resolve("backups");
        Files.createDirectories(databasePath.getParent());
        Files.writeString(databasePath, "sqlite-data");
        Clock fixedClock = Clock.fixed(Instant.parse("2026-06-30T12:00:00Z"), ZoneId.of("UTC"));

        Path backupFile = PreFlywayDatabaseBackup.backupIfDatabaseExists(databasePath, backupDirectory, fixedClock).orElseThrow();

        assertThat(backupFile.getFileName().toString()).matches("eliteseriespay-\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}\\.db");
        assertThat(Files.readString(backupFile)).isEqualTo("sqlite-data");
    }
}
