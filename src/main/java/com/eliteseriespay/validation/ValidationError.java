package com.eliteseriespay.validation;

public enum ValidationError {

    PROJECT_NAME_REQUIRED("Укажите название"),
    EPISODE_COST_REQUIRED("Укажите стоимость эпизода"),
    EPISODE_COST_NOT_POSITIVE("Стоимость эпизода должна быть больше нуля"),
    VK_ID_REQUIRED("Укажите VK ID"),
    VK_ID_ALREADY_EXISTS("VK ID уже используется другим участником"),
    PARTICIPANT_NAME_REQUIRED("Укажите имя"),
    PARTICIPANT_ALREADY_ACTIVE("Участник уже состоит в проекте"),
    NOT_AN_ACTIVE_MEMBER("Участник не является активным членом проекта");

    private final String message;

    ValidationError(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
