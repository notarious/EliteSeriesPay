package com.eliteseriespay.backup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatabaseBackupServiceTest {

    private static final DateTimeFormatter BACKUP_FILE_NAME_FORMAT =
            DateTimeFormatter.ofPattern("'eliteseriespay-'yyyy-MM-dd-HH-mm-ss'.db'");

    @TempDir
    Path tempDir;

    private Path databasePath;
    private Path backupDirectory;
    private DatabaseBackupProperties properties;
    private Clock clock;

    @BeforeEach
    void setUp() {
        databasePath = tempDir.resolve("eliteseriespay.db");
        backupDirectory = tempDir.resolve("backups");

        properties = new DatabaseBackupProperties();
        properties.setDatabasePath(databasePath.toString());
        properties.setBackupDirectory(backupDirectory.toString());
        properties.setRetentionLimit(20);
        properties.setStartupEnabled(true);

        clock = Clock.fixed(Instant.parse("2026-06-26T15:30:45Z"), ZoneId.systemDefault());
    }

    @Test
    void createBackup_createsBackupFile() throws IOException {
        Files.writeString(databasePath, "sqlite-data");

        DatabaseBackupService service = new DatabaseBackupService(properties, clock);
        Path backupFile = service.createBackup();

        assertThat(backupFile).exists();
        assertThat(backupFile.getFileName().toString())
                .isEqualTo(expectedBackupFileName(Instant.parse("2026-06-26T15:30:45Z")));
        assertThat(Files.readString(backupFile)).isEqualTo("sqlite-data");
    }

    @Test
    void createBackup_enforcesRetentionLimit() throws IOException {
        Files.writeString(databasePath, "sqlite-data");
        properties.setRetentionLimit(20);
        DatabaseBackupService service = new DatabaseBackupService(properties, clock);

        IntStream.rangeClosed(1, 25).forEach(index -> {
            Instant backupInstant = Instant.parse("2026-01-" + String.format("%02d", index) + "T10:00:00Z");
            Clock backupClock = Clock.fixed(backupInstant, ZoneId.systemDefault());
            DatabaseBackupService backupService = new DatabaseBackupService(properties, backupClock);
            backupService.createBackup();
        });

        try (var files = Files.list(backupDirectory)) {
            List<String> backupFileNames = files
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();

            assertThat(backupFileNames).hasSize(20);
            assertThat(backupFileNames.getFirst())
                    .isEqualTo(expectedBackupFileName(Instant.parse("2026-01-06T10:00:00Z")));
            assertThat(backupFileNames.getLast())
                    .isEqualTo(expectedBackupFileName(Instant.parse("2026-01-25T10:00:00Z")));
        }
    }

    @Test
    void createBackup_skipsMissingDatabaseFile() {
        DatabaseBackupService service = new DatabaseBackupService(properties, clock);

        assertThatThrownBy(service::createBackup)
                .isInstanceOf(DatabaseBackupException.class)
                .hasFieldOrPropertyWithValue("userMessage", DatabaseBackupException.SOURCE_MISSING);
    }

    @Test
    void backupOnStartup_skipsMissingDatabaseFile() {
        DatabaseBackupService service = new DatabaseBackupService(properties, clock);

        Optional<Path> backupFile = service.backupOnStartup();

        assertThat(backupFile).isEmpty();
        assertThat(backupDirectory).doesNotExist();
    }

    @Test
    void createBackup_rejectsEmptyDatabaseFile() throws IOException {
        Files.createFile(databasePath);

        DatabaseBackupService service = new DatabaseBackupService(properties, clock);

        assertThatThrownBy(service::createBackup)
                .isInstanceOf(DatabaseBackupException.class)
                .hasFieldOrPropertyWithValue("userMessage", DatabaseBackupException.SOURCE_EMPTY);
    }

    @Test
    void backupOnStartup_skipsEmptyDatabaseFile() throws IOException {
        Files.createFile(databasePath);

        DatabaseBackupService service = new DatabaseBackupService(properties, clock);

        Optional<Path> backupFile = service.backupOnStartup();

        assertThat(backupFile).isEmpty();
        assertThat(backupDirectory).doesNotExist();
    }

    @Test
    void listBackups_returnsExistingBackupsSortedByNewestFirst() throws IOException {
        Files.createDirectories(backupDirectory);
        Path olderBackup = backupDirectory.resolve("eliteseriespay-2026-06-25-10-00-00.db");
        Path newerBackup = backupDirectory.resolve("eliteseriespay-2026-06-26-12-00-00.db");
        Files.writeString(olderBackup, "older");
        Files.writeString(newerBackup, "newer");

        DatabaseBackupService service = new DatabaseBackupService(properties, clock);
        List<DatabaseBackupInfo> backups = service.listBackups();

        assertThat(backups).hasSize(2);
        assertThat(backups.get(0).fileName()).isEqualTo("eliteseriespay-2026-06-26-12-00-00.db");
        assertThat(backups.get(0).sizeBytes()).isEqualTo(5);
        assertThat(backups.get(0).sizeFormatted()).isEqualTo("5 Б");
        assertThat(backups.get(1).fileName()).isEqualTo("eliteseriespay-2026-06-25-10-00-00.db");
        assertThat(backups.get(1).sizeBytes()).isEqualTo(5);
    }

    @Test
    void listBackups_ignoresUnrelatedFiles() throws IOException {
        Files.createDirectories(backupDirectory);
        Files.writeString(backupDirectory.resolve("eliteseriespay-2026-06-26-12-00-00.db"), "backup");
        Files.writeString(backupDirectory.resolve("notes.txt"), "ignore");

        DatabaseBackupService service = new DatabaseBackupService(properties, clock);

        assertThat(service.listBackups()).hasSize(1);
    }

    private String expectedBackupFileName(Instant instant) {
        return BACKUP_FILE_NAME_FORMAT.format(instant.atZone(ZoneId.systemDefault()));
    }
}
