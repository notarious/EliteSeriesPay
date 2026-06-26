package com.eliteseriespay.exchangerate;

import com.eliteseriespay.domain.PaymentCurrency;

public interface ExchangeRateClient {

    /**
     * Returns how many RUB equal one unit of the given foreign currency.
     */
    ExchangeRateQuote fetchRateToRub(PaymentCurrency currency);
}
