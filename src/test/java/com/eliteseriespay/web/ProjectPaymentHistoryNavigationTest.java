package com.eliteseriespay.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.eliteseriespay.domain.BillingMode;
import com.eliteseriespay.domain.PaymentCurrency;
import com.eliteseriespay.domain.PaymentSource;
import com.eliteseriespay.domain.PaymentStatus;
import com.eliteseriespay.payment.history.ProjectPaymentHistoryFilter;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ProjectPaymentHistoryNavigationTest {

    @Test
    void listUrl_preservesFiltersAndPageSize() {
        ProjectPaymentHistoryFilter filter = ProjectPaymentHistoryFilter.of(
                5L,
                10L,
                BillingMode.SUBSCRIPTION,
                PaymentSource.VK_DONUT,
                PaymentCurrency.RUB,
                PaymentStatus.ACTIVE,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 6, 1),
                2,
                50);

        String url = ProjectPaymentHistoryNavigation.of(5L, filter).listUrl();

        assertThat(url).contains("/projects/5/payments");
        assertThat(url).contains("participantId=10");
        assertThat(url).contains("billingMode=SUBSCRIPTION");
        assertThat(url).contains("source=VK_DONUT");
        assertThat(url).contains("currency=RUB");
        assertThat(url).contains("status=ACTIVE");
        assertThat(url).contains("dateFrom=2026-01-01");
        assertThat(url).contains("dateTo=2026-06-01");
        assertThat(url).contains("page=2");
        assertThat(url).contains("size=50");
    }

    @Test
    void editUrl_includesProjectHistoryRoundTripParams() {
        ProjectPaymentHistoryFilter filter = ProjectPaymentHistoryFilter.of(
                5L, null, BillingMode.PACKAGE, null, null, null, null, null, 1, 25);

        String url = ProjectPaymentHistoryNavigation.of(5L, filter).editUrl(10L, 99L);

        assertThat(url).contains("/participants/10/payments/99/edit");
        assertThat(url).contains("projectHistoryProjectId=5");
        assertThat(url).contains("projectHistoryBillingMode=PACKAGE");
        assertThat(url).contains("projectHistoryPage=1");
    }
}
