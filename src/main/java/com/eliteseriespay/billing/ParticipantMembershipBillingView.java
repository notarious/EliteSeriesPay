package com.eliteseriespay.billing;

import com.eliteseriespay.domain.BillingMode;
import com.eliteseriespay.domain.Project;
import com.eliteseriespay.domain.SubscriptionPaymentStatus;
import java.time.YearMonth;

public record ParticipantMembershipBillingView(Project project,
                                               BillingMode billingMode,
                                               String billingModeLabel,
                                               YearMonth paidUntilMonth,
                                               String paymentStatusLabel,
                                               String statusCssClass,
                                               boolean overdue,
                                               PartialPaymentInfo partialPaymentInfo) {

    public static ParticipantMembershipBillingView forSubscription(Project project,
                                                                   YearMonth paidUntilMonth,
                                                                   SubscriptionPaymentStatus status,
                                                                   PartialPaymentInfo partialPaymentInfo) {
        return new ParticipantMembershipBillingView(
                project,
                BillingMode.SUBSCRIPTION,
                "Абонемент",
                paidUntilMonth,
                ProjectParticipantBillingView.subscriptionStatusLabel(status),
                ProjectParticipantBillingView.subscriptionStatusCssClass(status),
                status == SubscriptionPaymentStatus.OVERDUE
                        || status == SubscriptionPaymentStatus.NO_PAYMENTS,
                partialPaymentInfo);
    }

    public static ParticipantMembershipBillingView forPackage(Project project) {
        return new ParticipantMembershipBillingView(
                project,
                BillingMode.PACKAGE,
                "Пакет",
                null,
                "Пакет",
                "",
                false,
                null);
    }
}
