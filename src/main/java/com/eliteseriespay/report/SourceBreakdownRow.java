package com.eliteseriespay.report;

import com.eliteseriespay.domain.PaymentSource;
import java.math.BigDecimal;

public record SourceBreakdownRow(String sourceLabel,
                                 PaymentSource source,
                                 long paymentCount,
                                 String grossAmountRub,
                                 String netAmountRub) {
}
