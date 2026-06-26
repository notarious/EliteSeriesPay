package com.eliteseriespay.validation;

public enum ValidationError {

    PROJECT_NAME_REQUIRED("Укажите название"),
    EPISODE_COST_REQUIRED("Укажите стоимость эпизода"),
    EPISODE_COST_NOT_POSITIVE("Стоимость эпизода должна быть больше нуля"),
    MONTHLY_FEE_RUB_REQUIRED("Укажите абонентскую плату в рублях"),
    MONTHLY_FEE_RUB_NOT_POSITIVE("Абонентская плата в рублях должна быть больше нуля"),
    MONTHLY_FEE_EUR_REQUIRED("Укажите абонентскую плату в евро"),
    MONTHLY_FEE_EUR_NOT_POSITIVE("Абонентская плата в евро должна быть больше нуля"),
    BILLING_MODE_REQUIRED("Выберите тип оплаты"),
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
    EXCHANGE_RATE_NOT_POSITIVE("Курс должен быть больше нуля"),
    EXCHANGE_RATE_UNSUPPORTED_CURRENCY("Курс можно получить только для доллара или евро"),
    PAYMENT_VOIDED("Аннулированный платёж нельзя изменить"),
    PAYMENT_ALREADY_VOIDED("Платёж уже аннулирован"),
    INITIAL_SUBSCRIPTION_PAYMENT_INSUFFICIENT(
            "Для вступления в проект необходимо оплатить полный месячный абонемент"),
    INITIAL_SUBSCRIPTION_PAYMENT_USD_NOT_SUPPORTED(
            "Для первого платежа по абонементу выберите рубли или евро");

    private final String message;

    ValidationError(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
