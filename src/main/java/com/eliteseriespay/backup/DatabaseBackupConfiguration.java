package com.eliteseriespay.backup;

import java.time.Clock;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DatabaseBackupProperties.class)
public class DatabaseBackupConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    ApplicationRunner databaseBackupOnStartupRunner(DatabaseBackupService databaseBackupService) {
        return arguments -> databaseBackupService.backupOnStartup();
    }
}
