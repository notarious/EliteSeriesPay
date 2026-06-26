package com.eliteseriespay.service;

import com.eliteseriespay.domain.PaymentCurrency;
import com.eliteseriespay.exchangerate.ExchangeRateClient;
import com.eliteseriespay.exchangerate.ExchangeRateFetchException;
import com.eliteseriespay.exchangerate.ExchangeRateQuote;
import com.eliteseriespay.exception.ValidationException;
import com.eliteseriespay.validation.ValidationError;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class ExchangeRateService {

    private static final int EXCHANGE_RATE_SCALE = 4;

    private final ExchangeRateClient exchangeRateClient;

    public ExchangeRateService(ExchangeRateClient exchangeRateClient) {
        this.exchangeRateClient = exchangeRateClient;
    }

    public ExchangeRateQuote getRateToRub(PaymentCurrency currency) {
        if (currency == null) {
            throw new ValidationException(ValidationError.EXCHANGE_RATE_UNSUPPORTED_CURRENCY);
        }
        if (currency == PaymentCurrency.RUB) {
            throw new ValidationException(ValidationError.EXCHANGE_RATE_UNSUPPORTED_CURRENCY);
        }

        try {
            ExchangeRateQuote quote = exchangeRateClient.fetchRateToRub(currency);
            BigDecimal normalizedRate = quote.rate()
                    .setScale(EXCHANGE_RATE_SCALE, RoundingMode.HALF_UP);
            return new ExchangeRateQuote(currency, normalizedRate);
        } catch (ExchangeRateFetchException ex) {
            throw new ExchangeRateFetchException(ExchangeRateFetchException.USER_MESSAGE, ex);
        }
    }
}
