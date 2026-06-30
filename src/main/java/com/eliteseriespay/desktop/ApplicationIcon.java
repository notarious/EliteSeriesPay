package com.eliteseriespay.desktop;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Taskbar;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ApplicationIcon {

    private static final Logger log = LoggerFactory.getLogger(ApplicationIcon.class);

    private static final String ICON_RESOURCE = "/icons/eliteseriespay.png";

    private ApplicationIcon() {
    }

    public static BufferedImage loadImage() {
        URL resource = ApplicationIcon.class.getResource(ICON_RESOURCE);
        if (resource == null) {
            throw new IllegalStateException("Application icon not found on classpath: " + ICON_RESOURCE);
        }

        try (InputStream inputStream = resource.openStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new IllegalStateException("Unable to decode application icon: " + ICON_RESOURCE);
            }
            return image;
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to load application icon: " + ICON_RESOURCE, exception);
        }
    }

    public static BufferedImage loadScaledImage(int size) {
        BufferedImage source = loadImage();
        BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaled.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(source, 0, 0, size, size, null);
        } finally {
            graphics.dispose();
        }
        return scaled;
    }

    public static void applyToDesktopTaskbarIfSupported() {
        if (!Taskbar.isTaskbarSupported()) {
            return;
        }

        Taskbar taskbar = Taskbar.getTaskbar();
        if (!taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
            return;
        }

        try {
            taskbar.setIconImage(loadImage());
        } catch (Exception exception) {
            log.warn("Unable to set taskbar icon: {}", exception.getMessage());
        }
    }
}
