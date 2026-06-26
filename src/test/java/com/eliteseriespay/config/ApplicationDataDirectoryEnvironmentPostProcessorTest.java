package com.eliteseriespay.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
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

        Path expectedDatabase = tempDir.resolve("eliteseriespay.db");
        Path expectedBackups = tempDir.resolve("backups");

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo(ApplicationDataDirectory.toJdbcSqliteFileUrl(expectedDatabase));
        assertThat(environment.getProperty("eliteseriespay.database-backup.database-path"))
                .isEqualTo(expectedDatabase.toString());
        assertThat(environment.getProperty("eliteseriespay.database-backup.backup-directory"))
                .isEqualTo(expectedBackups.toString());
        assertThat(Files.isDirectory(expectedBackups)).isTrue();
    }

    @Test
    void postProcessEnvironment_keepsDevelopmentDefaults() {
        MockEnvironment environment = new MockEnvironment();

        new ApplicationDataDirectoryEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.url")).isNull();
        assertThat(environment.getProperty("eliteseriespay.database-backup.database-path")).isNull();
    }
}
