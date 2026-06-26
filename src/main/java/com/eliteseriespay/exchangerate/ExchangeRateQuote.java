package com.eliteseriespay.exchangerate;

import com.eliteseriespay.domain.PaymentCurrency;
import java.math.BigDecimal;

public record ExchangeRateQuote(PaymentCurrency currency, BigDecimal rate) {
}
