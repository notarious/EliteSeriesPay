package com.eliteseriespay.domain;

public enum PaymentCurrency {

    RUB("₽"),
    USD("$"),
    EUR("€");

    private final String displayName;

    PaymentCurrency(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
