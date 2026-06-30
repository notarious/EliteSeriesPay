package com.eliteseriespay.config;

import java.nio.file.Path;

public record DatabaseLocationMigrationResult(
        Status status,
        Path legacyPath,
        Path targetPath) {

    public enum Status {
        NOT_NEEDED,
        SKIPPED_DESTINATION_EXISTS,
        MOVED
    }

    public static DatabaseLocationMigrationResult notNeeded(Path legacyPath, Path targetPath) {
        return new DatabaseLocationMigrationResult(Status.NOT_NEEDED, legacyPath, targetPath);
    }

    public static DatabaseLocationMigrationResult skippedDestinationExists(Path legacyPath, Path targetPath) {
        return new DatabaseLocationMigrationResult(Status.SKIPPED_DESTINATION_EXISTS, legacyPath, targetPath);
    }

    public static DatabaseLocationMigrationResult moved(Path legacyPath, Path targetPath) {
        return new DatabaseLocationMigrationResult(Status.MOVED, legacyPath, targetPath);
    }
}
