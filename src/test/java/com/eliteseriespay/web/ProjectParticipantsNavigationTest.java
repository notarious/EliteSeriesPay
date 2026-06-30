package com.eliteseriespay.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.eliteseriespay.billing.MembershipPaymentStatusFilter;
import com.eliteseriespay.domain.BillingMode;
import org.junit.jupiter.api.Test;

class ProjectParticipantsNavigationTest {

    @Test
    void listUrl_preservesFilters() {
        ProjectParticipantsFilter filter = ProjectParticipantsFilter.of(
                BillingMode.SUBSCRIPTION,
                MembershipPaymentStatusFilter.OVERDUE);

        String url = ProjectParticipantsNavigation.listUrl(7L, filter);

        assertThat(url).isEqualTo("/projects/7?billingMode=SUBSCRIPTION&paymentStatus=OVERDUE");
    }

    @Test
    void listUrl_omitsAllPaymentStatusFilter() {
        ProjectParticipantsFilter filter = ProjectParticipantsFilter.of(
                BillingMode.PACKAGE,
                MembershipPaymentStatusFilter.ALL);

        String url = ProjectParticipantsNavigation.listUrl(7L, filter);

        assertThat(url).isEqualTo("/projects/7?billingMode=PACKAGE");
    }

    @Test
    void formParams_includesActiveFiltersOnly() {
        ProjectParticipantsFilter filter = ProjectParticipantsFilter.of(
                BillingMode.SUBSCRIPTION,
                MembershipPaymentStatusFilter.ACTIVE);

        assertThat(ProjectParticipantsNavigation.of(7L, filter).formParams())
                .containsExactly(
                        new PaymentHistoryFormParam("billingMode", BillingMode.SUBSCRIPTION),
                        new PaymentHistoryFormParam("paymentStatus", MembershipPaymentStatusFilter.ACTIVE));
    }
}
