package com.eliteseriespay.desktop;

import com.eliteseriespay.config.ApplicationDataDirectory;
import java.nio.file.Path;
import org.springframework.core.env.Environment;
import org.springframework.core.env.ConfigurableEnvironment;

public final class DesktopEnvironment {

    static final String DEFAULT_APPLICATION_HOST = "http://localhost";

    private DesktopEnvironment() {
    }

    public static boolean isTrayEnabled(DesktopProperties properties, Environment environment) {
        if (properties.getTrayEnabled() != null) {
            return properties.getTrayEnabled();
        }
        return isPackagedDesktopTarget(environment);
    }

    public static boolean isSingleInstanceEnabled(DesktopProperties properties, Environment environment) {
        if (properties.getSingleInstanceEnabled() != null) {
            return properties.getSingleInstanceEnabled();
        }
        return isPackagedDesktopTarget(environment);
    }

    public static boolean isSingleInstanceEnabledPreSpring() {
        return ApplicationDataDirectory.isPackagedPreSpring()
                && ApplicationDataDirectory.isWindows();
    }

    public static Path resolveDataDirectoryPreSpring() {
        return ApplicationDataDirectory.resolvePreSpring();
    }

    public static String applicationUrl(Environment environment) {
        int port = environment.getProperty("server.port", Integer.class, 8080);
        return DEFAULT_APPLICATION_HOST + ":" + port;
    }

    public static String applicationUrl(int port) {
        return DEFAULT_APPLICATION_HOST + ":" + port;
    }

    private static boolean isPackagedDesktopTarget(Environment environment) {
        return ApplicationDataDirectory.isPackaged(asConfigurable(environment))
                && ApplicationDataDirectory.isWindows();
    }

    private static ConfigurableEnvironment asConfigurable(Environment environment) {
        if (environment instanceof ConfigurableEnvironment configurableEnvironment) {
            return configurableEnvironment;
        }
        throw new IllegalStateException("Expected ConfigurableEnvironment");
    }
}
