package com.eliteseriespay.service;

import com.eliteseriespay.domain.SubscriptionPaymentStatus;

public record SubscriptionPaymentStatusFilter(SubscriptionPaymentStatus status) {

    public static final SubscriptionPaymentStatusFilter ALL = new SubscriptionPaymentStatusFilter(null);

    public static SubscriptionPaymentStatusFilter of(SubscriptionPaymentStatus status) {
        if (status == null) {
            return ALL;
        }
        return new SubscriptionPaymentStatusFilter(status);
    }

    public boolean isAll() {
        return status == null;
    }
}
