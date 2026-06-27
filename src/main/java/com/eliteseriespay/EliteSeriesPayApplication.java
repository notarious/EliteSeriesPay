package com.eliteseriespay;

import com.eliteseriespay.desktop.DesktopEnvironment;
import com.eliteseriespay.desktop.DesktopSupport;
import com.eliteseriespay.desktop.SingleInstanceLock;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EliteSeriesPayApplication {

    private static final Logger log = LoggerFactory.getLogger(EliteSeriesPayApplication.class);

    private static SingleInstanceLock instanceLock;

    public static void main(String[] args) {
        if (DesktopEnvironment.isSingleInstanceEnabledPreSpring()) {
            instanceLock = SingleInstanceLock.forDataDirectory(DesktopEnvironment.resolveDataDirectoryPreSpring());
            try {
                if (!instanceLock.tryAcquire()) {
                    DesktopSupport.openApplicationInBrowser(DesktopEnvironment.applicationUrl(8080));
                    System.exit(0);
                }
            } catch (IOException exception) {
                log.warn("Unable to acquire single-instance lock: {}", exception.getMessage());
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (instanceLock != null) {
                        instanceLock.close();
                    }
                } catch (IOException exception) {
                    log.debug("Unable to release single-instance lock: {}", exception.getMessage());
                }
            }));
        }

        SpringApplication.run(EliteSeriesPayApplication.class, args);
    }
}
