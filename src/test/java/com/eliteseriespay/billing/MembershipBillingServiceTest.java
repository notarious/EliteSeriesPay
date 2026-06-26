package com.eliteseriespay.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.eliteseriespay.domain.BillingMode;
import com.eliteseriespay.domain.MembershipStatus;
import com.eliteseriespay.domain.Participant;
import com.eliteseriespay.domain.PaymentCurrency;
import com.eliteseriespay.domain.PaymentSource;
import com.eliteseriespay.domain.Project;
import com.eliteseriespay.domain.ProjectMembership;
import com.eliteseriespay.domain.SubscriptionPaymentStatus;
import com.eliteseriespay.repository.PaymentRepository;
import com.eliteseriespay.repository.ProjectMembershipRepository;
import com.eliteseriespay.support.TestEntities;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MembershipBillingServiceTest {

    private static final YearMonth CURRENT_MONTH = YearMonth.of(2026, 6);

    @Mock
    private ProjectMembershipRepository projectMembershipRepository;

    @Mock
    private PaymentRepository paymentRepository;

    private MembershipBillingService membershipBillingService;
    private MembershipBillingCalculator calculator;
    private Project project;
    private Participant participant;

    @BeforeEach
    void setUp() {
        calculator = new MembershipBillingCalculator();
        membershipBillingService = new MembershipBillingService(
                projectMembershipRepository, paymentRepository, calculator);
        project = TestEntities.project(1L, "Series", new BigDecimal("1000.00"),
                new BigDecimal("500.00"), new BigDecimal("5.00"));
        participant = TestEntities.participant(10L, "12345", "Ivan", null);
    }

    @Test
    void buildProjectParticipantViews_filtersByOverdueIncludingNoPayments() {
        ProjectMembership overdue = TestEntities.membership(
                project, participant, MembershipStatus.ACTIVE, BillingMode.SUBSCRIPTION);
        Participant packageParticipant = TestEntities.participant(11L, "67890", "Petr", null);
        ProjectMembership packageMembership = TestEntities.membership(
                project, packageParticipant, MembershipStatus.ACTIVE, BillingMode.PACKAGE);

        List<ProjectParticipantBillingView> views = membershipBillingService.buildProjectParticipantViews(
                List.of(overdue, packageMembership),
                BillingModeFilter.ALL,
                MembershipPaymentStatusFilter.OVERDUE,
                CURRENT_MONTH);

        assertThat(views).hasSize(1);
        assertThat(views.getFirst().participant()).isEqualTo(participant);
        assertThat(views.getFirst().paymentStatusLabel()).isEqualTo("Просрочен");
    }

    @Test
    void buildProjectParticipantViews_filtersPackageParticipants() {
        ProjectMembership subscription = TestEntities.membership(
                project, participant, MembershipStatus.ACTIVE, BillingMode.SUBSCRIPTION);
        subscription.updateBilling(CURRENT_MONTH, null, null);
        Participant packageParticipant = TestEntities.participant(11L, "67890", "Petr", null);
        ProjectMembership packageMembership = TestEntities.membership(
                project, packageParticipant, MembershipStatus.ACTIVE, BillingMode.PACKAGE);

        List<ProjectParticipantBillingView> views = membershipBillingService.buildProjectParticipantViews(
                List.of(subscription, packageMembership),
                BillingModeFilter.ALL,
                MembershipPaymentStatusFilter.PACKAGE,
                CURRENT_MONTH);

        assertThat(views).hasSize(1);
        assertThat(views.getFirst().billingMode()).isEqualTo(BillingMode.PACKAGE);
        assertThat(views.getFirst().paymentStatusLabel()).isEqualTo("Пакет");
    }

    @Test
    void buildProjectParticipantViews_hidesPartialPaymentWhenSubscriptionActive() {
        ProjectMembership membership = TestEntities.membership(
                project, participant, MembershipStatus.ACTIVE, BillingMode.SUBSCRIPTION);
        membership.updateBilling(CURRENT_MONTH, new BigDecimal("100.00"), PaymentCurrency.RUB);

        List<ProjectParticipantBillingView> views = membershipBillingService.buildProjectParticipantViews(
                List.of(membership),
                BillingModeFilter.ALL,
                MembershipPaymentStatusFilter.ALL,
                CURRENT_MONTH);

        assertThat(views.getFirst().subscriptionPaymentStatus()).isEqualTo(SubscriptionPaymentStatus.ACTIVE);
        assertThat(views.getFirst().partialPaymentInfo()).isNull();
    }

    @Test
    void buildProjectParticipantViews_showsPartialPaymentWhenSubscriptionNotActive() {
        ProjectMembership membership = TestEntities.membership(
                project, participant, MembershipStatus.ACTIVE, BillingMode.SUBSCRIPTION);
        membership.updateBilling(null, new BigDecimal("100.00"), PaymentCurrency.RUB);

        List<ProjectParticipantBillingView> views = membershipBillingService.buildProjectParticipantViews(
                List.of(membership),
                BillingModeFilter.ALL,
                MembershipPaymentStatusFilter.ALL,
                CURRENT_MONTH);

        assertThat(views.getFirst().subscriptionPaymentStatus()).isEqualTo(SubscriptionPaymentStatus.NO_PAYMENTS);
        assertThat(views.getFirst().partialPaymentInfo()).isNotNull();
        assertThat(views.getFirst().partialPaymentInfo().advanceAmount())
                .isEqualByComparingTo("100.00");
    }

    @Test
    void buildParticipantMembershipViews_hidesPartialPaymentWhenSubscriptionActive() {
        ProjectMembership membership = TestEntities.membership(
                project, participant, MembershipStatus.ACTIVE, BillingMode.SUBSCRIPTION);
        membership.updateBilling(CURRENT_MONTH, new BigDecimal("100.00"), PaymentCurrency.RUB);

        List<ParticipantMembershipBillingView> views = membershipBillingService.buildParticipantMembershipViews(
                List.of(membership), CURRENT_MONTH);

        assertThat(views.getFirst().paymentStatusLabel()).isEqualTo("Активен");
        assertThat(views.getFirst().partialPaymentInfo()).isNull();
    }

    @Test
    void buildParticipantMembershipViews_showsPartialPaymentWhenSubscriptionOverdue() {
        ProjectMembership membership = TestEntities.membership(
                project, participant, MembershipStatus.ACTIVE, BillingMode.SUBSCRIPTION);
        membership.updateBilling(CURRENT_MONTH.minusMonths(1), new BigDecimal("100.00"), PaymentCurrency.RUB);

        List<ParticipantMembershipBillingView> views = membershipBillingService.buildParticipantMembershipViews(
                List.of(membership), CURRENT_MONTH);

        assertThat(views.getFirst().paymentStatusLabel()).isEqualTo("Просрочен");
        assertThat(views.getFirst().partialPaymentInfo()).isNotNull();
        assertThat(views.getFirst().partialPaymentInfo().advanceAmount())
                .isEqualByComparingTo("100.00");
    }

    @Test
    void recalculateBilling_afterVoidingPayment_mayMakeParticipantOverdue() {
        ProjectMembership membership = TestEntities.membership(
                project, participant, MembershipStatus.ACTIVE, BillingMode.SUBSCRIPTION);
        membership.updateBilling(CURRENT_MONTH, null, null);
        when(paymentRepository.findActivePaymentsByProjectAndParticipant(1L, 10L))
                .thenReturn(List.of());

        membershipBillingService.recalculateBilling(membership);

        assertThat(membership.getPaidUntilMonth()).isNull();
        assertThat(calculator.resolveSubscriptionStatus(membership.getPaidUntilMonth(), CURRENT_MONTH))
                .isEqualTo(SubscriptionPaymentStatus.NO_PAYMENTS);
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
    }

    @Test
    void recalculateBilling_afterPaymentEdit_updatesPaidUntilMonth() {
        ProjectMembership membership = TestEntities.membership(
                project, participant, MembershipStatus.ACTIVE, BillingMode.SUBSCRIPTION);
        var payment = TestEntities.payment(
                1L, participant, project, LocalDate.of(2026, 6, 15), PaymentSource.MANUAL,
                new BigDecimal("500.00"), PaymentCurrency.RUB, new BigDecimal("1.0000"),
                new BigDecimal("500.00"), 0, new BigDecimal("500.00"), null);
        when(paymentRepository.findActivePaymentsByProjectAndParticipant(1L, 10L))
                .thenReturn(List.of(payment));

        membershipBillingService.recalculateBilling(membership);

        assertThat(membership.getPaidUntilMonth()).isEqualTo(CURRENT_MONTH);
        assertThat(calculator.resolveSubscriptionStatus(membership.getPaidUntilMonth(), CURRENT_MONTH))
                .isEqualTo(SubscriptionPaymentStatus.ACTIVE);
    }

    @Test
    void buildParticipantMembershipViews_showsOverdueForExpiredSubscription() {
        ProjectMembership membership = TestEntities.membership(
                project, participant, MembershipStatus.ACTIVE, BillingMode.SUBSCRIPTION);
        membership.updateBilling(CURRENT_MONTH.minusMonths(2), null, null);

        List<ParticipantMembershipBillingView> views = membershipBillingService.buildParticipantMembershipViews(
                List.of(membership), CURRENT_MONTH);

        assertThat(views.getFirst().overdue()).isTrue();
        assertThat(views.getFirst().paymentStatusLabel()).isEqualTo("Просрочен");
        assertThat(views.getFirst().paidUntilMonth()).isEqualTo(CURRENT_MONTH.minusMonths(2));
    }
}
