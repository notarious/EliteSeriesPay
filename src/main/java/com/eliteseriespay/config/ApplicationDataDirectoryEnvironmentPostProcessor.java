package com.eliteseriespay.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
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

        try {
            Files.createDirectories(backupsDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to create application data directory: " + dataDirectory, exception);
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.datasource.url", ApplicationDataDirectory.toJdbcSqliteFileUrl(databaseFile));
        properties.put("eliteseriespay.database-backup.database-path", databaseFile.toString());
        properties.put("eliteseriespay.database-backup.backup-directory", backupsDirectory.toString());

        environment
                .getPropertySources()
                .addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
    }
}
