package com.eliteseriespay.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class ApplicationDataDirectoryEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "applicationDataDirectory";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!ApplicationDataDirectory.shouldOverrideDefaultPaths(environment)) {
            return;
        }

        Path dataDirectory = ApplicationDataDirectory.resolve(environment);
        Path databaseFile = ApplicationDataDirectory.databasePath(dataDirectory);
        Path backupsDirectory = ApplicationDataDirectory.backupsDirectory(dataDirectory);
        Path logsDirectory = ApplicationDataDirectory.logsDirectory(dataDirectory);
        String jdbcUrl = ApplicationDataDirectory.toJdbcSqliteFileUrl(databaseFile);

        try {
            Files.createDirectories(dataDirectory);
            Files.createDirectories(backupsDirectory);
            Files.createDirectories(logsDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to create application data directory: " + dataDirectory, exception);
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.datasource.url", jdbcUrl);
        properties.put("eliteseriespay.database-backup.database-path", databaseFile.toString());
        properties.put("eliteseriespay.database-backup.backup-directory", backupsDirectory.toString());
        properties.put("logging.file.name", logsDirectory.resolve("application.log").toString());

        environment
                .getPropertySources()
                .addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));

        PackagedStartupDiagnostics.logResolvedPaths(environment, dataDirectory, databaseFile, backupsDirectory, jdbcUrl);
        application.addListeners(logResolvedPaths(dataDirectory, databaseFile, backupsDirectory, jdbcUrl));
    }

    private static ApplicationListener<ApplicationEnvironmentPreparedEvent> logResolvedPaths(
            Path dataDirectory, Path databaseFile, Path backupsDirectory, String jdbcUrl) {
        return event -> {
            Logger logger = LoggerFactory.getLogger(ApplicationDataDirectoryEnvironmentPostProcessor.class);
            logger.info("Application data directory: {}", dataDirectory.toAbsolutePath().normalize());
            logger.info("Database path: {}", databaseFile.toAbsolutePath().normalize());
            logger.info("JDBC URL: {}", jdbcUrl);
            logger.info("Backups directory: {}", backupsDirectory.toAbsolutePath().normalize());
            logger.info("Logs directory: {}", ApplicationDataDirectory.logsDirectory(dataDirectory).toAbsolutePath().normalize());
        };
    }
}
