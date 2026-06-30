package com.eliteseriespay.backup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PreFlywayDatabaseBackup {

    private static final Logger log = LoggerFactory.getLogger(PreFlywayDatabaseBackup.class);

    private PreFlywayDatabaseBackup() {
    }

    public static Optional<Path> backupIfDatabaseExists(Path databasePath, Path backupDirectory) {
        return backupIfDatabaseExists(databasePath, backupDirectory, Clock.systemDefaultZone());
    }

    public static Optional<Path> backupIfDatabaseExists(Path databasePath, Path backupDirectory, Clock clock) {
        if (!Files.isRegularFile(databasePath)) {
            return Optional.empty();
        }

        try {
            if (Files.size(databasePath) == 0L) {
                return Optional.empty();
            }

            Files.createDirectories(backupDirectory);
            Path backupFile = backupDirectory.resolve(
                    DatabaseBackupFiles.generateBackupFileName(Instant.now(clock)));
            Files.copy(databasePath, backupFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("Pre-Flyway database backup created: {}", backupFile.toAbsolutePath().normalize());
            return Optional.of(backupFile);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to create pre-Flyway database backup for: " + databasePath, exception);
        }
    }
}
