package com.eliteseriespay.web.controllers;

import com.eliteseriespay.backup.DatabaseBackupException;
import com.eliteseriespay.backup.DatabaseBackupService;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/backups")
public class DatabaseBackupController {

    private final DatabaseBackupService databaseBackupService;

    public DatabaseBackupController(DatabaseBackupService databaseBackupService) {
        this.databaseBackupService = databaseBackupService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("backups", databaseBackupService.listBackups());
        model.addAttribute("databaseFileName", databaseBackupService.getDatabaseFileName());
        return "backups/list";
    }

    @PostMapping
    public String create(RedirectAttributes redirectAttributes) {
        try {
            Path backupFile = databaseBackupService.createBackup();
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Резервная копия создана: " + backupFile.getFileName());
        } catch (DatabaseBackupException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getUserMessage());
        }

        return "redirect:/backups";
    }

    @GetMapping("/{fileName}/download")
    public ResponseEntity<Resource> download(@PathVariable String fileName) {
        Path backupFile = databaseBackupService.getBackupFileForDownload(fileName);
        Resource resource = new FileSystemResource(backupFile);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @ExceptionHandler(DatabaseBackupException.class)
    public String handleBackupError(DatabaseBackupException exception, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", exception.getUserMessage());
        return "redirect:/backups";
    }
}
