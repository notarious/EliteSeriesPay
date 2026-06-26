package com.eliteseriespay.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.eliteseriespay.domain.BillingMode;
import com.eliteseriespay.domain.MembershipStatus;
import com.eliteseriespay.domain.Participant;
import com.eliteseriespay.domain.PaymentCurrency;
import com.eliteseriespay.domain.PaymentSource;
import com.eliteseriespay.domain.Project;
import com.eliteseriespay.domain.ProjectMembership;
import com.eliteseriespay.domain.SubscriptionPaymentStatus;
import com.eliteseriespay.support.TestEntities;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MembershipBillingCalculatorTest {

    private static final BigDecimal MONTHLY_FEE_RUB = new BigDecimal("500.00");
    private static final BigDecimal MONTHLY_FEE_EUR = new BigDecimal("5.00");

    private MembershipBillingCalculator calculator;
    private Project project;
    private Participant participant;

    @BeforeEach
    void setUp() {
        calculator = new MembershipBillingCalculator();
        project = TestEntities.project(1L, "Series", new BigDecimal("1000.00"), MONTHLY_FEE_RUB, MONTHLY_FEE_EUR);
        participant = TestEntities.participant(10L, "12345", "Ivan", null);
    }

    @Test
    void fullMonthlyPayment_extendsPaidUntilMonthByOneMonth() {
        var payment = TestEntities.payment(
                1L, participant, project, LocalDate.of(2026, 6, 15), PaymentSource.MANUAL,
                MONTHLY_FEE_RUB, PaymentCurrency.RUB, new BigDecimal("1.0000"),
                MONTHLY_FEE_RUB, 0, MONTHLY_FEE_RUB, null);

        MembershipBillingState state = calculator.recalculateSubscription(project, List.of(payment));

        assertThat(state.paidUntilMonth()).isEqualTo(YearMonth.of(2026, 6));
        assertThat(state.hasPartial()).isFalse();
        assertThat(calculator.resolveSubscriptionStatus(state.paidUntilMonth(), YearMonth.of(2026, 6)))
                .isEqualTo(SubscriptionPaymentStatus.ACTIVE);
    }

    @Test
    void multiMonthSubscriptionPayment_extendsPaidUntilMonthBySeveralMonths() {
        var payment = TestEntities.payment(
                1L, participant, project, LocalDate.of(2026, 6, 1), PaymentSource.MANUAL,
                new BigDecimal("1500.00"), PaymentCurrency.RUB, new BigDecimal("1.0000"),
                new BigDecimal("1500.00"), 0, new BigDecimal("1500.00"), null);

        MembershipBillingState state = calculator.recalculateSubscription(project, List.of(payment));

        assertThat(state.paidUntilMonth()).isEqualTo(YearMonth.of(2026, 8));
        assertThat(state.hasPartial()).isFalse();
    }

    @Test
    void multiMonthPackagePayment_doesNotUpdateMembershipBillingFields() {
        ProjectMembership packageMembership = TestEntities.membership(
                project, participant, MembershipStatus.ACTIVE, BillingMode.PACKAGE);
        MembershipBillingService billingService = new MembershipBillingService(null, null, calculator);

        billingService.recalculateBilling(packageMembership);

        assertThat(packageMembership.getPaidUntilMonth()).isNull();
        assertThat(packageMembership.getPartialPaymentAmount()).isNull();
    }

    @Test
    void partialPayment_storesRemainderWithoutMakingParticipantActive() {
        var payment = TestEntities.payment(
                1L, participant, project, LocalDate.of(2026, 6, 1), PaymentSource.MANUAL,
                new BigDecimal("150.00"), PaymentCurrency.RUB, new BigDecimal("1.0000"),
                new BigDecimal("150.00"), 0, new BigDecimal("150.00"), null);

        MembershipBillingState state = calculator.recalculateSubscription(project, List.of(payment));

        assertThat(state.paidUntilMonth()).isNull();
        assertThat(state.partialPaymentAmount()).isEqualByComparingTo("150.00");
        assertThat(state.partialPaymentCurrency()).isEqualTo(PaymentCurrency.RUB);
        assertThat(calculator.resolveSubscriptionStatus(state.paidUntilMonth(), YearMonth.of(2026, 6)))
                .isEqualTo(SubscriptionPaymentStatus.NO_PAYMENTS);

        var partialInfo = calculator.resolvePartialPaymentInfo(
                project, state.partialPaymentAmount(), state.partialPaymentCurrency());
        assertThat(partialInfo).isPresent();
        assertThat(partialInfo.get().remainingUntilRenewal()).isEqualByComparingTo("350.00");
    }

    @Test
    void mixedPartialAndLaterCompletion_extendsPaidUntilMonth() {
        var partialPayment = TestEntities.payment(
                1L, participant, project, LocalDate.of(2026, 6, 1), PaymentSource.MANUAL,
                new BigDecimal("150.00"), PaymentCurrency.RUB, new BigDecimal("1.0000"),
                new BigDecimal("150.00"), 0, new BigDecimal("150.00"), null);
        var completionPayment = TestEntities.payment(
                2L, participant, project, LocalDate.of(2026, 6, 10), PaymentSource.MANUAL,
                new BigDecimal("350.00"), PaymentCurrency.RUB, new BigDecimal("1.0000"),
                new BigDecimal("350.00"), 0, new BigDecimal("350.00"), null);

        MembershipBillingState state = calculator.recalculateSubscription(
                project, List.of(partialPayment, completionPayment));

        assertThat(state.paidUntilMonth()).isEqualTo(YearMonth.of(2026, 6));
        assertThat(state.hasPartial()).isFalse();
        assertThat(calculator.resolveSubscriptionStatus(state.paidUntilMonth(), YearMonth.of(2026, 6)))
                .isEqualTo(SubscriptionPaymentStatus.ACTIVE);
    }

    @Test
    void overdueSubscriptionParticipant_isMarkedOverdue() {
        YearMonth paidUntil = YearMonth.of(2026, 4);

        assertThat(calculator.resolveSubscriptionStatus(paidUntil, YearMonth.of(2026, 6)))
                .isEqualTo(SubscriptionPaymentStatus.OVERDUE);
    }

    @Test
    void packageParticipant_isNeverOverdue() {
        ProjectParticipantBillingView view = ProjectParticipantBillingView.forPackage(participant);

        assertThat(view.paymentStatusLabel()).isEqualTo("Пакет");
        assertThat(view.subscriptionPaymentStatus()).isNull();
        assertThat(calculator.matchesPaymentStatus(
                BillingMode.PACKAGE, null, SubscriptionPaymentStatusFilter.of(SubscriptionPaymentStatus.OVERDUE)))
                .isFalse();
    }

    @Test
    void eurPayment_usesMonthlyFeeEur() {
        var payment = TestEntities.payment(
                1L, participant, project, LocalDate.of(2026, 6, 1), PaymentSource.MANUAL,
                new BigDecimal("10.00"), PaymentCurrency.EUR, new BigDecimal("90.0000"),
                new BigDecimal("900.00"), 0, new BigDecimal("900.00"), null);

        MembershipBillingState state = calculator.recalculateSubscription(project, List.of(payment));

        assertThat(state.paidUntilMonth()).isEqualTo(YearMonth.of(2026, 7));
        assertThat(state.hasPartial()).isFalse();
    }
}
