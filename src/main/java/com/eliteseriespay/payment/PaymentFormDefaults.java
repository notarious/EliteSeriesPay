package com.eliteseriespay.payment;

import com.eliteseriespay.domain.PaymentCurrency;
import com.eliteseriespay.domain.PaymentSource;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentFormDefaults(
        LocalDate paymentDate,
        Long projectId,
        PaymentSource source,
        BigDecimal amountOriginal,
        PaymentCurrency currency,
        BigDecimal exchangeRate) {
}
