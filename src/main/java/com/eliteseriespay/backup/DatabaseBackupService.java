package com.eliteseriespay.backup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DatabaseBackupService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseBackupService.class);

    static final Pattern BACKUP_FILE_NAME_PATTERN =
            Pattern.compile("eliteseriespay-\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}\\.db");

    private static final DateTimeFormatter BACKUP_FILE_NAME_FORMAT =
            DateTimeFormatter.ofPattern("'eliteseriespay-'yyyy-MM-dd-HH-mm-ss'.db'");

    private static final DateTimeFormatter DISPLAY_DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private final DatabaseBackupProperties properties;
    private final Clock clock;

    public DatabaseBackupService(DatabaseBackupProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public Optional<Path> backupOnStartup() {
        if (!properties.isStartupEnabled()) {
            return Optional.empty();
        }

        try {
            return createBackupIfPossible();
        } catch (Exception exception) {
            log.warn("Не удалось создать резервную копию при запуске: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    public Path createBackup() {
        Path databasePath = resolveDatabasePath();
        if (!Files.exists(databasePath)) {
            throw new DatabaseBackupException(DatabaseBackupException.SOURCE_MISSING);
        }

        try {
            if (isEmptyDatabase(databasePath)) {
                throw new DatabaseBackupException(DatabaseBackupException.SOURCE_EMPTY);
            }
            return performBackup(databasePath);
        } catch (DatabaseBackupException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new DatabaseBackupException(DatabaseBackupException.CREATE_FAILED, exception);
        }
    }

    public List<DatabaseBackupInfo> listBackups() {
        Path backupDirectory = resolveBackupDirectory();
        if (!Files.isDirectory(backupDirectory)) {
            return List.of();
        }

        try (Stream<Path> files = Files.list(backupDirectory)) {
            return files
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(fileName -> BACKUP_FILE_NAME_PATTERN.matcher(fileName).matches())
                    .sorted(Comparator.reverseOrder())
                    .map(fileName -> toBackupInfo(backupDirectory.resolve(fileName), fileName))
                    .toList();
        } catch (IOException exception) {
            throw new DatabaseBackupException(DatabaseBackupException.LIST_FAILED, exception);
        }
    }

    public Path getBackupFileForDownload(String fileName) {
        if (!BACKUP_FILE_NAME_PATTERN.matcher(fileName).matches()) {
            throw new DatabaseBackupException(DatabaseBackupException.NOT_FOUND);
        }

        Path backupDirectory = resolveBackupDirectory().toAbsolutePath().normalize();
        Path backupFile = backupDirectory.resolve(fileName).normalize();
        if (!backupFile.startsWith(backupDirectory) || !Files.isRegularFile(backupFile)) {
            throw new DatabaseBackupException(DatabaseBackupException.NOT_FOUND);
        }

        return backupFile;
    }

    public String getDatabaseFileName() {
        return resolveDatabasePath().getFileName().toString();
    }

    private Optional<Path> createBackupIfPossible() throws IOException {
        Path databasePath = resolveDatabasePath();
        if (!Files.exists(databasePath) || isEmptyDatabase(databasePath)) {
            return Optional.empty();
        }

        return Optional.of(performBackup(databasePath));
    }

    private Path performBackup(Path databasePath) throws IOException {
        Path backupDirectory = ensureBackupDirectory();
        Path backupFile = backupDirectory.resolve(generateBackupFileName());
        Files.copy(databasePath, backupFile, StandardCopyOption.REPLACE_EXISTING);
        enforceRetention(backupDirectory);
        return backupFile;
    }

    private void enforceRetention(Path backupDirectory) throws IOException {
        try (Stream<Path> files = Files.list(backupDirectory)) {
            List<Path> backupFiles = files
                    .filter(Files::isRegularFile)
                    .filter(path -> BACKUP_FILE_NAME_PATTERN.matcher(path.getFileName().toString()).matches())
                    .sorted(Comparator.comparing(path -> path.getFileName().toString(), Comparator.reverseOrder()))
                    .toList();

            for (int index = properties.getRetentionLimit(); index < backupFiles.size(); index++) {
                Files.deleteIfExists(backupFiles.get(index));
            }
        }
    }

    private Path resolveDatabasePath() {
        return Path.of(properties.getDatabasePath()).normalize();
    }

    private Path resolveBackupDirectory() {
        return Path.of(properties.getBackupDirectory()).normalize();
    }

    private Path ensureBackupDirectory() throws IOException {
        Path backupDirectory = resolveBackupDirectory();
        Files.createDirectories(backupDirectory);
        return backupDirectory;
    }

    private String generateBackupFileName() {
        return BACKUP_FILE_NAME_FORMAT.format(Instant.now(clock).atZone(ZoneId.systemDefault()));
    }

    private boolean isEmptyDatabase(Path databasePath) throws IOException {
        return Files.size(databasePath) == 0L;
    }

    private DatabaseBackupInfo toBackupInfo(Path backupFile, String fileName) {
        try {
            BasicFileAttributes attributes = Files.readAttributes(backupFile, BasicFileAttributes.class);
            long sizeBytes = attributes.size();
            Instant createdAt = toInstant(attributes.lastModifiedTime());
            String createdAtFormatted = DISPLAY_DATE_TIME_FORMAT.format(createdAt.atZone(ZoneId.systemDefault()));

            return new DatabaseBackupInfo(fileName, createdAtFormatted, sizeBytes, formatFileSize(sizeBytes));
        } catch (IOException exception) {
            throw new DatabaseBackupException(DatabaseBackupException.LIST_FAILED, exception);
        }
    }

    private Instant toInstant(FileTime fileTime) {
        return fileTime.toInstant();
    }

    static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " Б";
        }
        if (bytes < 1024 * 1024) {
            return String.format(Locale.forLanguageTag("ru"), "%.1f КБ", bytes / 1024.0);
        }
        return String.format(Locale.forLanguageTag("ru"), "%.1f МБ", bytes / (1024.0 * 1024.0));
    }
}
