package com.eliteseriespay.payment.history;

import com.eliteseriespay.domain.BillingMode;
import com.eliteseriespay.domain.PaymentCurrency;
import com.eliteseriespay.domain.PaymentSource;
import com.eliteseriespay.domain.PaymentStatus;
import java.time.LocalDate;
import java.util.Set;

public record ProjectPaymentHistoryFilter(
        Long projectId,
        Long participantId,
        BillingMode billingMode,
        PaymentSource source,
        PaymentCurrency currency,
        PaymentStatus status,
        LocalDate dateFrom,
        LocalDate dateTo,
        int page,
        int pageSize) {

    public static final int DEFAULT_PAGE_SIZE = 25;
    public static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(25, 50, 100);

    public static ProjectPaymentHistoryFilter of(Long projectId,
                                                 Long participantId,
                                                 BillingMode billingMode,
                                                 PaymentSource source,
                                                 PaymentCurrency currency,
                                                 PaymentStatus status,
                                                 LocalDate dateFrom,
                                                 LocalDate dateTo,
                                                 int page,
                                                 int pageSize) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = ALLOWED_PAGE_SIZES.contains(pageSize) ? pageSize : DEFAULT_PAGE_SIZE;
        LocalDate normalizedFrom = dateFrom;
        LocalDate normalizedTo = dateTo;
        if (normalizedFrom != null && normalizedTo != null && normalizedFrom.isAfter(normalizedTo)) {
            LocalDate swap = normalizedFrom;
            normalizedFrom = normalizedTo;
            normalizedTo = swap;
        }
        return new ProjectPaymentHistoryFilter(
                projectId,
                participantId,
                billingMode,
                source,
                currency,
                status,
                normalizedFrom,
                normalizedTo,
                normalizedPage,
                normalizedSize);
    }

    public boolean hasActiveFilters() {
        return participantId != null
                || billingMode != null
                || source != null
                || currency != null
                || status != null
                || dateFrom != null
                || dateTo != null;
    }
}
