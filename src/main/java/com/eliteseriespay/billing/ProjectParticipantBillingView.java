package com.eliteseriespay.billing;

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
                                            String statusCssClass,
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
                subscriptionStatusCssClass(status),
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
                "",
                null);
    }

    public static String subscriptionStatusLabel(SubscriptionPaymentStatus status) {
        return switch (status) {
            case NO_PAYMENTS, OVERDUE -> "Просрочен";
            case ACTIVE -> "Активен";
        };
    }

    public static String subscriptionStatusCssClass(SubscriptionPaymentStatus status) {
        return switch (status) {
            case ACTIVE -> "text-success";
            case NO_PAYMENTS, OVERDUE -> "text-danger";
        };
    }
}
