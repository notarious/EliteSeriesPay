package com.eliteseriespay.service;

public enum ProjectValidationError {

    NAME_REQUIRED("Укажите название"),
    EPISODE_COST_REQUIRED("Укажите стоимость эпизода"),
    EPISODE_COST_NOT_POSITIVE("Стоимость эпизода должна быть больше нуля");

    private final String message;

    ProjectValidationError(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
