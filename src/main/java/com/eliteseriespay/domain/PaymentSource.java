package com.eliteseriespay.domain;

public enum PaymentSource {

    VK_DONUT("VK Donut (-10%)"),
    MANUAL("Вручную");

    private final String displayName;

    PaymentSource(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
