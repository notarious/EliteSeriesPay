package com.eliteseriespay.report;

import java.math.BigDecimal;

public record MonthlyPaymentTotals(long paymentCount,
                                   BigDecimal totalGrossRub,
                                   BigDecimal totalNetRub) {

    public static MonthlyPaymentTotals empty() {
        return new MonthlyPaymentTotals(0, BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2));
    }

    public boolean hasPayments() {
        return paymentCount > 0;
    }
}
