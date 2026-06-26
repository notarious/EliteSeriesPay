package com.eliteseriespay.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ParticipantPaymentSummary(
        LocalDate latestPaymentDate,
        String latestProjectName,
        BigDecimal latestAmountRub,
        BigDecimal latestNetAmountRub,
        BigDecimal totalNetAmountRub) {

    public boolean hasPayments() {
        return latestPaymentDate != null;
    }
}
