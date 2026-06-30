package com.eliteseriespay.config;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.env.ConfigurableEnvironment;

final class PackagedStartupDiagnostics {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private PackagedStartupDiagnostics() {
    }

    static void logPreSpring() {
        if (!ApplicationDataDirectory.isWindows() || !ApplicationDataDirectory.isPackagedPreSpring()) {
            return;
        }

        try {
            Path dataDirectory = ApplicationDataDirectory.resolvePreSpring();
            writeStartupLog(dataDirectory, buildPreSpringLines(dataDirectory));
        } catch (IOException exception) {
            System.err.println("Unable to write packaged startup log: " + exception.getMessage());
        }
    }

    static void logResolvedPaths(
            ConfigurableEnvironment environment,
            Path dataDirectory,
            Path databaseFile,
            Path backupsDirectory,
            Path applicationDirectory,
            String jdbcUrl) {
        if (!ApplicationDataDirectory.isWindows() || !ApplicationDataDirectory.isPackaged(environment)) {
            return;
        }

        try {
            writeStartupLog(dataDirectory, buildResolvedPathLines(
                    environment, dataDirectory, databaseFile, backupsDirectory, applicationDirectory, jdbcUrl));
        } catch (IOException exception) {
            System.err.println("Unable to write packaged startup log: " + exception.getMessage());
        }
    }

    private static List<String> buildPreSpringLines(Path dataDirectory) {
        Path applicationDirectory = ApplicationDataDirectory.applicationDirectory();
        return buildResolvedPathLines(
                null,
                dataDirectory,
                ApplicationDataDirectory.databasePath(dataDirectory),
                ApplicationDataDirectory.backupsDirectory(dataDirectory),
                applicationDirectory,
                "pending Spring environment configuration");
    }

    private static List<String> buildResolvedPathLines(
            ConfigurableEnvironment environment,
            Path dataDirectory,
            Path databaseFile,
            Path backupsDirectory,
            Path applicationDirectory,
            String jdbcUrl) {
        Path logsDirectory = ApplicationDataDirectory.logsDirectory(dataDirectory);
        Path lockFile = dataDirectory.resolve("eliteseriespay.instance.lock");
        List<String> lines = new ArrayList<>();
        lines.add("packaged property: " + readPackagedPropertyValue());
        lines.add("jpackage layout: " + ApplicationDataDirectory.isJPackageApplicationLayout());
        if (environment == null) {
            lines.add("packaged mode active: " + ApplicationDataDirectory.isPackagedPreSpring());
        } else {
            lines.add("packaged mode active: " + ApplicationDataDirectory.isPackaged(environment));
        }
        if (applicationDirectory != null) {
            lines.add("application directory: " + applicationDirectory.toAbsolutePath().normalize());
        }
        lines.add("user data directory: " + dataDirectory.toAbsolutePath().normalize());
        lines.add("database path: " + databaseFile.toAbsolutePath().normalize());
        lines.add("backup directory: " + backupsDirectory.toAbsolutePath().normalize());
        lines.add("lock file path: " + lockFile.toAbsolutePath().normalize());
        lines.add("logs directory: " + logsDirectory.toAbsolutePath().normalize());
        lines.add("working directory: " + Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize());
        lines.add("LOCALAPPDATA: " + System.getenv("LOCALAPPDATA"));
        lines.add("datasource URL: " + jdbcUrl);
        return lines;
    }

    private static String readPackagedPropertyValue() {
        return System.getProperty(ApplicationDataDirectory.PACKAGED_PROPERTY, "false");
    }

    private static void writeStartupLog(Path dataDirectory, List<String> lines) throws IOException {
        Path logsDirectory = ApplicationDataDirectory.logsDirectory(dataDirectory);
        Files.createDirectories(logsDirectory);
        Path startupLog = logsDirectory.resolve("startup.log");

        StringBuilder content = new StringBuilder();
        content.append("=== EliteSeriesPay startup ")
                .append(TIMESTAMP_FORMAT.format(Instant.now()))
                .append(" ===")
                .append(System.lineSeparator());
        for (String line : lines) {
            content.append(line).append(System.lineSeparator());
        }
        content.append(System.lineSeparator());

        Files.writeString(startupLog, content, java.nio.charset.StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND);
    }
}
