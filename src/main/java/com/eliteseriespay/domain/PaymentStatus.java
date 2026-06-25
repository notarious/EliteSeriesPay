package com.eliteseriespay.domain;

public enum PaymentStatus {

    ACTIVE("Активен"),
    VOIDED("Аннулирован");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
