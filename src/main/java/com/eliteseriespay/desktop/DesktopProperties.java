package com.eliteseriespay.desktop;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "eliteseriespay.desktop")
public class DesktopProperties {

    private Boolean trayEnabled;
    private Boolean singleInstanceEnabled;
    private boolean openBrowserOnStartup = true;

    public Boolean getTrayEnabled() {
        return trayEnabled;
    }

    public void setTrayEnabled(Boolean trayEnabled) {
        this.trayEnabled = trayEnabled;
    }

    public Boolean getSingleInstanceEnabled() {
        return singleInstanceEnabled;
    }

    public void setSingleInstanceEnabled(Boolean singleInstanceEnabled) {
        this.singleInstanceEnabled = singleInstanceEnabled;
    }

    public boolean isOpenBrowserOnStartup() {
        return openBrowserOnStartup;
    }

    public void setOpenBrowserOnStartup(boolean openBrowserOnStartup) {
        this.openBrowserOnStartup = openBrowserOnStartup;
    }
}
