package com.eliteseriespay.billing;

public enum MembershipPaymentStatusFilter {

    ALL("Все"),
    ACTIVE("Активные"),
    OVERDUE("Просроченные"),
    PACKAGE("Пакет");

    private final String displayName;

    MembershipPaymentStatusFilter(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
