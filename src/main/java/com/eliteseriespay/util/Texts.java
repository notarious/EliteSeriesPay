package com.eliteseriespay.util;

public final class Texts {

    private Texts() {
    }

    public static String trim(String value) {
        return value == null ? null : value.trim();
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
