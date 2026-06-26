package com.eliteseriespay.service;

import com.eliteseriespay.domain.BillingMode;
import com.eliteseriespay.domain.Participant;
import com.eliteseriespay.domain.SubscriptionPaymentStatus;
import java.time.YearMonth;

public record ProjectParticipantBillingView(Participant participant,
                                            BillingMode billingMode,
                                            String billingModeLabel,
                                            YearMonth paidUntilMonth,
                                            SubscriptionPaymentStatus subscriptionPaymentStatus,
                                            String paymentStatusLabel,
                                            PartialPaymentInfo partialPaymentInfo) {

    public static ProjectParticipantBillingView forSubscription(Participant participant,
                                                                YearMonth paidUntilMonth,
                                                                SubscriptionPaymentStatus status,
                                                                PartialPaymentInfo partialPaymentInfo) {
        return new ProjectParticipantBillingView(
                participant,
                BillingMode.SUBSCRIPTION,
                "Абонемент",
                paidUntilMonth,
                status,
                subscriptionStatusLabel(status),
                partialPaymentInfo);
    }

    public static ProjectParticipantBillingView forPackage(Participant participant) {
        return new ProjectParticipantBillingView(
                participant,
                BillingMode.PACKAGE,
                "Пакет",
                null,
                null,
                "Пакет",
                null);
    }

    private static String subscriptionStatusLabel(SubscriptionPaymentStatus status) {
        return switch (status) {
            case NO_PAYMENTS -> "Нет оплат";
            case ACTIVE -> "Активен";
            case OVERDUE -> "Просрочен";
        };
    }
}
