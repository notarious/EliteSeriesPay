package com.eliteseriespay.domain;

public enum BillingMode {

    SUBSCRIPTION("Абонемент"),
    PACKAGE("Пакет");

    private final String displayName;

    BillingMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
