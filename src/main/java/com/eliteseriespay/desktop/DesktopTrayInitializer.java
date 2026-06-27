package com.eliteseriespay.desktop;

import com.eliteseriespay.backup.DatabaseBackupException;
import com.eliteseriespay.backup.DatabaseBackupProperties;
import com.eliteseriespay.backup.DatabaseBackupService;
import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class DesktopTrayInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(DesktopTrayInitializer.class);

    private static final String TRAY_TOOLTIP = "EliteSeriesPay";
    private static final String STARTUP_NOTIFICATION_TITLE = "EliteSeriesPay запущен";
    private static final String STARTUP_NOTIFICATION_MESSAGE =
            "Приложение работает в фоновом режиме. Для выхода используйте значок в области уведомлений.";

    private final ConfigurableApplicationContext applicationContext;
    private final DesktopProperties desktopProperties;
    private final Environment environment;
    private final DatabaseBackupService databaseBackupService;
    private final DatabaseBackupProperties databaseBackupProperties;

    private TrayIcon trayIcon;

    public DesktopTrayInitializer(
            ConfigurableApplicationContext applicationContext,
            DesktopProperties desktopProperties,
            Environment environment,
            DatabaseBackupService databaseBackupService,
            DatabaseBackupProperties databaseBackupProperties) {
        this.applicationContext = applicationContext;
        this.desktopProperties = desktopProperties;
        this.environment = environment;
        this.databaseBackupService = databaseBackupService;
        this.databaseBackupProperties = databaseBackupProperties;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!DesktopEnvironment.isTrayEnabled(desktopProperties, environment)) {
            return;
        }

        if (!SystemTray.isSupported()) {
            log.warn("System tray is not supported on this platform. Continuing without tray icon.");
            openBrowserIfConfigured();
            return;
        }

        try {
            initializeTray();
        } catch (AWTException exception) {
            log.warn("Unable to initialize system tray: {}", exception.getMessage());
        }

        openBrowserIfConfigured();
    }

    private void initializeTray() throws AWTException {
        String applicationUrl = DesktopEnvironment.applicationUrl(environment);
        Image trayImage = DesktopTrayIconFactory.createTrayImage();
        PopupMenu popupMenu = createPopupMenu(applicationUrl);

        trayIcon = new TrayIcon(trayImage, TRAY_TOOLTIP, popupMenu);
        trayIcon.setImageAutoSize(true);

        SystemTray.getSystemTray().add(trayIcon);
        trayIcon.displayMessage(
                STARTUP_NOTIFICATION_TITLE,
                STARTUP_NOTIFICATION_MESSAGE,
                TrayIcon.MessageType.INFO);
    }

    private PopupMenu createPopupMenu(String applicationUrl) {
        PopupMenu popupMenu = new PopupMenu();

        MenuItem openApplicationItem = new MenuItem("Открыть приложение");
        openApplicationItem.addActionListener(event ->
                DesktopSupport.openApplicationInBrowser(applicationUrl));
        popupMenu.add(openApplicationItem);

        MenuItem openBackupsDirectoryItem = new MenuItem("Открыть папку резервных копий");
        openBackupsDirectoryItem.addActionListener(event -> openBackupsDirectory());
        popupMenu.add(openBackupsDirectoryItem);

        MenuItem createBackupItem = new MenuItem("Создать резервную копию");
        createBackupItem.addActionListener(event -> createBackupFromTray());
        popupMenu.add(createBackupItem);

        popupMenu.addSeparator();

        MenuItem exitItem = new MenuItem("Выход");
        exitItem.addActionListener(event -> exitApplication());
        popupMenu.add(exitItem);

        return popupMenu;
    }

    private void openBrowserIfConfigured() {
        if (!desktopProperties.isOpenBrowserOnStartup()) {
            return;
        }
        DesktopSupport.openApplicationInBrowser(DesktopEnvironment.applicationUrl(environment));
    }

    private void openBackupsDirectory() {
        Path backupDirectory = Path.of(databaseBackupProperties.getBackupDirectory()).normalize();
        DesktopSupport.openDirectory(backupDirectory);
    }

    private void createBackupFromTray() {
        Thread.startVirtualThread(() -> {
            try {
                Path backupFile = databaseBackupService.createBackup();
                displayTrayMessage("Резервная копия создана: " + backupFile.getFileName(), TrayIcon.MessageType.INFO);
            } catch (DatabaseBackupException exception) {
                displayTrayMessage(exception.getUserMessage(), TrayIcon.MessageType.ERROR);
            }
        });
    }

    private void exitApplication() {
        Thread.startVirtualThread(() -> {
            removeTrayIcon();
            int exitCode = SpringApplication.exit(applicationContext);
            System.exit(exitCode);
        });
    }

    private void removeTrayIcon() {
        if (trayIcon == null || !SystemTray.isSupported()) {
            return;
        }
        SystemTray.getSystemTray().remove(trayIcon);
    }

    private void displayTrayMessage(String message, TrayIcon.MessageType messageType) {
        if (trayIcon == null) {
            log.info(message);
            return;
        }
        trayIcon.displayMessage(TRAY_TOOLTIP, message, messageType);
    }
}
