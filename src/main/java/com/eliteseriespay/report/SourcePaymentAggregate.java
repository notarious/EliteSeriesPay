package com.eliteseriespay.report;

import com.eliteseriespay.domain.PaymentSource;
import java.math.BigDecimal;

public record SourcePaymentAggregate(PaymentSource source,
                                     long paymentCount,
                                     BigDecimal totalGrossRub,
                                     BigDecimal totalNetRub) {
}
