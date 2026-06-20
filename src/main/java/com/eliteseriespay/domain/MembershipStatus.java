package com.eliteseriespay.domain;

public enum MembershipStatus {

    ACTIVE("Активен"),
    LEFT("Исключён");

    private final String displayName;

    MembershipStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
