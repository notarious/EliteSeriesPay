package com.eliteseriespay.desktop;

import java.awt.Image;

final class DesktopTrayIconFactory {

    private static final int TRAY_ICON_SIZE = 16;

    private DesktopTrayIconFactory() {
    }

    static Image createTrayImage() {
        return ApplicationIcon.loadScaledImage(TRAY_ICON_SIZE);
    }
}
