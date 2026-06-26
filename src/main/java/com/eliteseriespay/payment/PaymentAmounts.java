package com.eliteseriespay.payment;

import java.math.BigDecimal;

public record PaymentAmounts(
        BigDecimal amountOriginal,
        BigDecimal exchangeRate,
        BigDecimal amountRub,
        int feePercent,
        BigDecimal netAmountRub) {
}
