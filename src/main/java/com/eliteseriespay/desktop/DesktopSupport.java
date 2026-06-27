package com.eliteseriespay.desktop;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DesktopSupport {

    private static final Logger log = LoggerFactory.getLogger(DesktopSupport.class);

    private DesktopSupport() {
    }

    public static void openApplicationInBrowser(String applicationUrl) {
        if (!Desktop.isDesktopSupported()) {
            log.warn("Desktop API is not supported. Unable to open {}", applicationUrl);
            return;
        }

        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.BROWSE)) {
            log.warn("Desktop browse action is not supported. Unable to open {}", applicationUrl);
            return;
        }

        try {
            desktop.browse(URI.create(applicationUrl));
        } catch (IOException exception) {
            log.warn("Unable to open {} in browser: {}", applicationUrl, exception.getMessage());
        }
    }

    public static void openDirectory(Path directory) {
        if (!Desktop.isDesktopSupported()) {
            log.warn("Desktop API is not supported. Unable to open directory {}", directory);
            return;
        }

        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.OPEN)) {
            log.warn("Desktop open action is not supported. Unable to open directory {}", directory);
            return;
        }

        try {
            Files.createDirectories(directory);
            desktop.open(directory.toFile());
        } catch (IOException exception) {
            log.warn("Unable to open directory {}: {}", directory, exception.getMessage());
        }
    }
}
