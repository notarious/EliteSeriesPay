package com.eliteseriespay.validation;

public enum ValidationError {

    PROJECT_NAME_REQUIRED("Укажите название"),
    EPISODE_COST_REQUIRED("Укажите стоимость эпизода"),
    EPISODE_COST_NOT_POSITIVE("Стоимость эпизода должна быть больше нуля"),
    VK_ID_REQUIRED("Укажите VK ID"),
    VK_ID_ALREADY_EXISTS("VK ID уже используется другим участником"),
    PARTICIPANT_NAME_REQUIRED("Укажите имя"),
    PARTICIPANT_ALREADY_ACTIVE("Участник уже состоит в проекте"),
    NOT_AN_ACTIVE_MEMBER("Участник не является активным членом проекта"),
    PAYMENT_DATE_REQUIRED("Укажите дату платежа"),
    PAYMENT_AMOUNT_REQUIRED("Укажите сумму"),
    PAYMENT_AMOUNT_NOT_POSITIVE("Сумма должна быть больше нуля"),
    PAYMENT_SOURCE_REQUIRED("Выберите источник"),
    PAYMENT_CURRENCY_REQUIRED("Выберите валюту"),
    EXCHANGE_RATE_REQUIRED("Укажите курс"),
    EXCHANGE_RATE_NOT_POSITIVE("Курс должен быть больше нуля");

    private final String message;

    ValidationError(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
