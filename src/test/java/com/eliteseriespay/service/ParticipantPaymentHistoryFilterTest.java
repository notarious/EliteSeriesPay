package com.eliteseriespay.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.eliteseriespay.domain.PaymentSource;
import com.eliteseriespay.domain.PaymentStatus;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ParticipantPaymentHistoryFilterTest {

    @Test
    void of_normalizesInvalidPageAndSize() {
        ParticipantPaymentHistoryFilter filter = ParticipantPaymentHistoryFilter.of(
                null, null, null, null, null, -2, 200);

        assertThat(filter.page()).isZero();
        assertThat(filter.pageSize()).isEqualTo(ParticipantPaymentHistoryFilter.DEFAULT_PAGE_SIZE);
    }

    @Test
    void of_swapsInvertedDateRange() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 1, 1);

        ParticipantPaymentHistoryFilter filter = ParticipantPaymentHistoryFilter.of(
                null, null, null, from, to, 0, 25);

        assertThat(filter.dateFrom()).isEqualTo(to);
        assertThat(filter.dateTo()).isEqualTo(from);
    }

    @Test
    void hasActiveFilters_detectsAnyFilterValue() {
        assertThat(ParticipantPaymentHistoryFilter.of(null, null, null, null, null, 0, 25).hasActiveFilters())
                .isFalse();
        assertThat(ParticipantPaymentHistoryFilter.of(
                1L, PaymentSource.MANUAL, PaymentStatus.ACTIVE,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), 0, 25).hasActiveFilters())
                .isTrue();
    }
}
