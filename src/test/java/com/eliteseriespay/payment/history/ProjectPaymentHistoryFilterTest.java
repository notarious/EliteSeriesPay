package com.eliteseriespay.payment.history;

import static org.assertj.core.api.Assertions.assertThat;

import com.eliteseriespay.domain.BillingMode;
import com.eliteseriespay.domain.PaymentCurrency;
import com.eliteseriespay.domain.PaymentSource;
import com.eliteseriespay.domain.PaymentStatus;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ProjectPaymentHistoryFilterTest {

    @Test
    void of_normalizesInvalidPageAndSize() {
        ProjectPaymentHistoryFilter filter = ProjectPaymentHistoryFilter.of(
                1L, null, null, null, null, null, null, null, -2, 200);

        assertThat(filter.page()).isZero();
        assertThat(filter.pageSize()).isEqualTo(ProjectPaymentHistoryFilter.DEFAULT_PAGE_SIZE);
    }

    @Test
    void of_swapsInvertedDateRange() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 1, 1);

        ProjectPaymentHistoryFilter filter = ProjectPaymentHistoryFilter.of(
                1L, null, null, null, null, null, from, to, 0, 25);

        assertThat(filter.dateFrom()).isEqualTo(to);
        assertThat(filter.dateTo()).isEqualTo(from);
    }

    @Test
    void hasActiveFilters_detectsAnyFilterValue() {
        assertThat(ProjectPaymentHistoryFilter.of(1L, null, null, null, null, null, null, null, 0, 25)
                .hasActiveFilters()).isFalse();
        assertThat(ProjectPaymentHistoryFilter.of(
                1L, 10L, BillingMode.PACKAGE, PaymentSource.MANUAL, PaymentCurrency.EUR,
                PaymentStatus.ACTIVE, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), 0, 25)
                .hasActiveFilters()).isTrue();
    }
}
