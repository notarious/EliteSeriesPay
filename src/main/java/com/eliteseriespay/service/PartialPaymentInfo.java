package com.eliteseriespay.service;

import com.eliteseriespay.domain.PaymentCurrency;
import java.math.BigDecimal;

public record PartialPaymentInfo(BigDecimal advanceAmount,
                                 PaymentCurrency advanceCurrency,
                                 BigDecimal remainingUntilRenewal) {
}
