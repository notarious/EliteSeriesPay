package com.eliteseriespay.backup;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

final class DatabaseBackupFiles {

    static final Pattern BACKUP_FILE_NAME_PATTERN =
            Pattern.compile("eliteseriespay-\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}\\.db");

    private static final DateTimeFormatter BACKUP_FILE_NAME_FORMAT =
            DateTimeFormatter.ofPattern("'eliteseriespay-'yyyy-MM-dd-HH-mm-ss'.db'");

    private DatabaseBackupFiles() {
    }

    static String generateBackupFileName(Instant instant) {
        return BACKUP_FILE_NAME_FORMAT.format(instant.atZone(ZoneId.systemDefault()));
    }
}
