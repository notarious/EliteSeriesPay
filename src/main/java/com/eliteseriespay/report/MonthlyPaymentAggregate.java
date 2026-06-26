package com.eliteseriespay.report;

import java.math.BigDecimal;

public record MonthlyPaymentAggregate(long paymentCount,
                                      BigDecimal totalGrossRub,
                                      BigDecimal totalNetRub) {

    public MonthlyPaymentTotals toTotals() {
        return new MonthlyPaymentTotals(
                paymentCount,
                totalGrossRub != null ? totalGrossRub : BigDecimal.ZERO,
                totalNetRub != null ? totalNetRub : BigDecimal.ZERO);
    }
}
