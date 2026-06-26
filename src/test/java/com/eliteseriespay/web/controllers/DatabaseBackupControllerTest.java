package com.eliteseriespay.web.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.eliteseriespay.backup.DatabaseBackupException;
import com.eliteseriespay.backup.DatabaseBackupProperties;
import com.eliteseriespay.backup.DatabaseBackupService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DatabaseBackupControllerTest {

    private static final DateTimeFormatter BACKUP_FILE_NAME_FORMAT =
            DateTimeFormatter.ofPattern("'eliteseriespay-'yyyy-MM-dd-HH-mm-ss'.db'");

    private static final Instant BACKUP_INSTANT = Instant.parse("2026-06-26T12:00:00Z");

    @TempDir
    Path tempDir;

    private Path databasePath;
    private Path backupDirectory;
    private DatabaseBackupService databaseBackupService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        databasePath = tempDir.resolve("eliteseriespay.db");
        backupDirectory = tempDir.resolve("backups");

        DatabaseBackupProperties properties = new DatabaseBackupProperties();
        properties.setDatabasePath(databasePath.toString());
        properties.setBackupDirectory(backupDirectory.toString());

        Clock clock = Clock.fixed(BACKUP_INSTANT, ZoneId.systemDefault());
        databaseBackupService = new DatabaseBackupService(properties, clock);
        mockMvc = MockMvcBuilders.standaloneSetup(new DatabaseBackupController(databaseBackupService)).build();
    }

    @Test
    void list_rendersBackupPage() throws Exception {
        mockMvc.perform(get("/backups"))
                .andExpect(status().isOk())
                .andExpect(view().name("backups/list"));
    }

    @Test
    void create_redirectsWithSuccessMessage() throws Exception {
        Files.writeString(databasePath, "sqlite-data");
        String backupFileName = expectedBackupFileName();

        mockMvc.perform(post("/backups"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/backups"))
                .andExpect(flash().attribute(
                        "successMessage",
                        "Резервная копия создана: " + backupFileName));
    }

    @Test
    void create_redirectsWithErrorMessageWhenDatabaseMissing() throws Exception {
        mockMvc.perform(post("/backups"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/backups"))
                .andExpect(flash().attribute("errorMessage", DatabaseBackupException.SOURCE_MISSING));
    }

    @Test
    void download_returnsBackupFile() throws Exception {
        Files.writeString(databasePath, "backup-content");
        String backupFileName = expectedBackupFileName();
        databaseBackupService.createBackup();

        mockMvc.perform(get("/backups/" + backupFileName + "/download"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Disposition",
                        "attachment; filename=\"" + backupFileName + "\""))
                .andExpect(content().bytes("backup-content".getBytes()));
    }

    @Test
    void download_handlesMissingBackup() throws Exception {
        mockMvc.perform(get("/backups/eliteseriespay-2026-06-26-12-00-00.db/download"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/backups"))
                .andExpect(flash().attribute("errorMessage", DatabaseBackupException.NOT_FOUND));
    }

    private String expectedBackupFileName() {
        return BACKUP_FILE_NAME_FORMAT.format(BACKUP_INSTANT.atZone(ZoneId.systemDefault()));
    }
}
