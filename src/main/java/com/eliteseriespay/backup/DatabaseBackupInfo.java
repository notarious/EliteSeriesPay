package com.eliteseriespay.backup;

public record DatabaseBackupInfo(
        String fileName,
        String createdAtFormatted,
        long sizeBytes,
        String sizeFormatted
) {
}
