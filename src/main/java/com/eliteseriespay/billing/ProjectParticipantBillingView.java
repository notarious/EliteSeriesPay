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
                                            PartialPaymentInfo partialPaymentInfo,
                                            CurrentMonthPaymentInfo currentMonthPayment) {

    public static ProjectParticipantBillingView forSubscription(Participant participant,
                                                                YearMonth paidUntilMonth,
                                                                SubscriptionPaymentStatus status,
                                                                PartialPaymentInfo partialPaymentInfo,
                                                                CurrentMonthPaymentInfo currentMonthPayment) {
        return new ProjectParticipantBillingView(
                participant,
                BillingMode.SUBSCRIPTION,
                BillingMode.SUBSCRIPTION.getDisplayName(),
                paidUntilMonth,
                status,
                subscriptionStatusLabel(status),
                subscriptionStatusCssClass(status),
                partialPaymentInfo,
                currentMonthPayment);
    }

    public static ProjectParticipantBillingView forPackage(Participant participant) {
        return new ProjectParticipantBillingView(
                participant,
                BillingMode.PACKAGE,
                BillingMode.PACKAGE.getDisplayName(),
                null,
                null,
                BillingMode.PACKAGE.getDisplayName(),
                "badge text-bg-secondary",
                null,
                CurrentMonthPaymentInfo.notApplicable());
    }

    public static String subscriptionStatusLabel(SubscriptionPaymentStatus status) {
        return switch (status) {
            case NO_PAYMENTS, OVERDUE -> "Просрочен";
            case ACTIVE -> "Активен";
        };
    }

    public static String subscriptionStatusCssClass(SubscriptionPaymentStatus status) {
        return switch (status) {
            case ACTIVE -> "badge text-bg-success";
            case NO_PAYMENTS, OVERDUE -> "badge text-bg-danger";
        };
    }
}
