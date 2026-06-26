package com.eliteseriespay.billing;

import com.eliteseriespay.domain.BillingMode;
import com.eliteseriespay.domain.Payment;
import com.eliteseriespay.domain.PaymentCurrency;
import com.eliteseriespay.domain.Project;
import com.eliteseriespay.domain.SubscriptionPaymentStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class MembershipBillingCalculator {

    public MembershipBillingState recalculateSubscription(Project project, List<Payment> activePayments) {
        MembershipBillingState state = MembershipBillingState.empty();

        activePayments.stream()
                .sorted(Comparator.comparing(Payment::getPaymentDate).thenComparing(Payment::getId))
                .forEach(payment -> applySubscriptionPayment(project, state, payment));

        return state;
    }

    public void applySubscriptionPayment(Project project, MembershipBillingState state, Payment payment) {
        SubscriptionAmount subscriptionAmount = resolveSubscriptionAmount(project, payment);
        if (subscriptionAmount == null) {
            return;
        }

        BigDecimal total = subscriptionAmount.amount();
        PaymentCurrency currency = subscriptionAmount.currency();

        if (state.hasPartial() && state.partialPaymentCurrency() == currency) {
            total = total.add(state.partialPaymentAmount());
        }

        BigDecimal monthlyFee = subscriptionAmount.monthlyFee();
        int fullMonths = total.divideToIntegralValue(monthlyFee).intValue();
        BigDecimal remainder = total.remainder(monthlyFee).setScale(2, RoundingMode.HALF_UP);

        if (fullMonths > 0) {
            YearMonth base = state.paidUntilMonth() != null
                    ? state.paidUntilMonth()
                    : YearMonth.from(payment.getPaymentDate()).minusMonths(1);
            state.setPaidUntilMonth(base.plusMonths(fullMonths));
        }

        if (remainder.compareTo(BigDecimal.ZERO) > 0) {
            state.setPartialPayment(remainder, currency);
        } else {
            state.clearPartialPayment();
        }
    }

    public boolean hasActivePaidUntilMonth(YearMonth paidUntilMonth, YearMonth currentMonth) {
        return paidUntilMonth != null && paidUntilMonth.compareTo(currentMonth) >= 0;
    }

    public SubscriptionPaymentStatus resolveSubscriptionStatus(YearMonth paidUntilMonth, YearMonth currentMonth) {
        if (paidUntilMonth == null) {
            return SubscriptionPaymentStatus.NO_PAYMENTS;
        }
        if (paidUntilMonth.compareTo(currentMonth) >= 0) {
            return SubscriptionPaymentStatus.ACTIVE;
        }
        return SubscriptionPaymentStatus.OVERDUE;
    }

    public Optional<PartialPaymentInfo> resolvePartialPaymentInfo(Project project,
                                                                  BigDecimal partialAmount,
                                                                  PaymentCurrency partialCurrency) {
        if (partialAmount == null || partialAmount.compareTo(BigDecimal.ZERO) <= 0 || partialCurrency == null) {
            return Optional.empty();
        }

        BigDecimal monthlyFee = switch (partialCurrency) {
            case RUB -> project.getMonthlyFeeRub();
            case EUR -> project.getMonthlyFeeEur();
            case USD -> project.getMonthlyFeeRub();
        };

        BigDecimal remainingUntilRenewal = monthlyFee.subtract(partialAmount).setScale(2, RoundingMode.HALF_UP);
        return Optional.of(new PartialPaymentInfo(partialAmount, partialCurrency, remainingUntilRenewal));
    }

    public boolean matchesBillingMode(BillingMode membershipMode, BillingModeFilter filter) {
        return filter == BillingModeFilter.ALL || filter.mode() == membershipMode;
    }

    public boolean matchesMembershipPaymentStatusFilter(BillingMode membershipMode,
                                                        SubscriptionPaymentStatus status,
                                                        MembershipPaymentStatusFilter filter) {
        return switch (filter) {
            case ALL -> true;
            case PACKAGE -> membershipMode == BillingMode.PACKAGE;
            case ACTIVE -> membershipMode == BillingMode.SUBSCRIPTION
                    && status == SubscriptionPaymentStatus.ACTIVE;
            case OVERDUE -> membershipMode == BillingMode.SUBSCRIPTION
                    && (status == SubscriptionPaymentStatus.OVERDUE
                    || status == SubscriptionPaymentStatus.NO_PAYMENTS);
        };
    }

    private SubscriptionAmount resolveSubscriptionAmount(Project project, Payment payment) {
        return switch (payment.getCurrency()) {
            case RUB -> new SubscriptionAmount(
                    payment.getAmountOriginal(), PaymentCurrency.RUB, project.getMonthlyFeeRub());
            case EUR -> new SubscriptionAmount(
                    payment.getAmountOriginal(), PaymentCurrency.EUR, project.getMonthlyFeeEur());
            case USD -> new SubscriptionAmount(
                    payment.getAmountRub(), PaymentCurrency.RUB, project.getMonthlyFeeRub());
        };
    }

    private record SubscriptionAmount(BigDecimal amount, PaymentCurrency currency, BigDecimal monthlyFee) {
    }
}
