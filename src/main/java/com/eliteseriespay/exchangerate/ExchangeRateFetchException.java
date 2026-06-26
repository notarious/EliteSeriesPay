package com.eliteseriespay.exchangerate;

public class ExchangeRateFetchException extends RuntimeException {

    public static final String USER_MESSAGE = "Не удалось получить курс. Укажите курс вручную.";

    public ExchangeRateFetchException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExchangeRateFetchException(String message) {
        super(message);
    }
}
