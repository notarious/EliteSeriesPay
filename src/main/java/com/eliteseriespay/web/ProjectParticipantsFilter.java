package com.eliteseriespay.web;

import com.eliteseriespay.billing.MembershipPaymentStatusFilter;
import com.eliteseriespay.domain.BillingMode;

public record ProjectParticipantsFilter(BillingMode billingMode,
                                        MembershipPaymentStatusFilter paymentStatus) {

    public static ProjectParticipantsFilter of(BillingMode billingMode,
                                               MembershipPaymentStatusFilter paymentStatus) {
        return new ProjectParticipantsFilter(billingMode, paymentStatus);
    }

    public static ProjectParticipantsFilter empty() {
        return new ProjectParticipantsFilter(null, null);
    }
}
