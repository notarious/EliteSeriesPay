package com.eliteseriespay.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ParticipantPaymentSummary(
        LocalDate latestPaymentDate,
        BigDecimal latestNetAmountRub,
        BigDecimal totalNetAmountRub) {

    public boolean hasPayments() {
        return latestPaymentDate != null;
    }
}
