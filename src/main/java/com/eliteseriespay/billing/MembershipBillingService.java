package com.eliteseriespay.billing;

import com.eliteseriespay.domain.BillingMode;
import com.eliteseriespay.domain.Payment;
import com.eliteseriespay.domain.PaymentCurrency;
import com.eliteseriespay.domain.ProjectMembership;
import com.eliteseriespay.domain.SubscriptionPaymentStatus;
import com.eliteseriespay.report.ReportFormatter;
import com.eliteseriespay.repository.PaymentRepository;
import com.eliteseriespay.repository.ProjectMembershipRepository;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembershipBillingService {

    private final ProjectMembershipRepository projectMembershipRepository;
    private final PaymentRepository paymentRepository;
    private final MembershipBillingCalculator membershipBillingCalculator;
    private final ReportFormatter reportFormatter;

    public MembershipBillingService(ProjectMembershipRepository projectMembershipRepository,
                                    PaymentRepository paymentRepository,
                                    MembershipBillingCalculator membershipBillingCalculator,
                                    ReportFormatter reportFormatter) {
        this.projectMembershipRepository = projectMembershipRepository;
        this.paymentRepository = paymentRepository;
        this.membershipBillingCalculator = membershipBillingCalculator;
        this.reportFormatter = reportFormatter;
    }

    @Transactional
    public void recalculateBilling(Long projectId, Long participantId) {
        projectMembershipRepository.findByProject_IdAndParticipant_Id(projectId, participantId)
                .ifPresent(this::recalculateBilling);
    }

    @Transactional
    public void recalculateBilling(ProjectMembership membership) {
        if (membership.getBillingMode() == BillingMode.PACKAGE) {
            return;
        }

        List<Payment> activePayments = paymentRepository.findActivePaymentsByProjectAndParticipant(
                membership.getProject().getId(), membership.getParticipant().getId());

        MembershipBillingState state = membershipBillingCalculator.recalculateSubscription(
                membership.getProject(), activePayments);

        membership.updateBilling(
                state.paidUntilMonth(),
                state.partialPaymentAmount(),
                state.partialPaymentCurrency());
    }

    @Transactional(readOnly = true)
    public String currentMonthPaymentColumnTitle(YearMonth currentMonth) {
        return reportFormatter.formatCurrentMonthPaymentColumnTitle(currentMonth);
    }

    @Transactional(readOnly = true)
    public List<ProjectParticipantBillingView> buildProjectParticipantViews(
            List<ProjectMembership> memberships,
            BillingModeFilter billingModeFilter,
            MembershipPaymentStatusFilter paymentStatusFilter,
            YearMonth currentMonth) {

        return memberships.stream()
                .filter(membership -> membershipBillingCalculator.matchesBillingMode(
                        membership.getBillingMode(), billingModeFilter))
                .map(membership -> toProjectBillingView(membership, currentMonth))
                .filter(view -> membershipBillingCalculator.matchesMembershipPaymentStatusFilter(
                        view.billingMode(), view.subscriptionPaymentStatus(), paymentStatusFilter))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ParticipantMembershipBillingView> buildParticipantMembershipViews(
            List<ProjectMembership> memberships,
            YearMonth currentMonth) {
        return memberships.stream()
                .map(membership -> toParticipantBillingView(membership, currentMonth))
                .toList();
    }

    private ProjectParticipantBillingView toProjectBillingView(ProjectMembership membership,
                                                                 YearMonth currentMonth) {
        if (membership.getBillingMode() == BillingMode.PACKAGE) {
            return ProjectParticipantBillingView.forPackage(membership.getParticipant());
        }

        SubscriptionPaymentStatus status = membershipBillingCalculator.resolveSubscriptionStatus(
                membership.getPaidUntilMonth(), currentMonth);
        Optional<PartialPaymentInfo> partialPaymentInfo = membershipBillingCalculator.resolvePartialPaymentInfo(
                membership.getProject(),
                membership.getPartialPaymentAmount(),
                membership.getPartialPaymentCurrency());

        return ProjectParticipantBillingView.forSubscription(
                membership.getParticipant(),
                membership.getPaidUntilMonth(),
                status,
                displayPartialPaymentInfo(status, partialPaymentInfo.orElse(null)),
                resolveCurrentMonthPayment(membership, currentMonth));
    }

    private ParticipantMembershipBillingView toParticipantBillingView(ProjectMembership membership,
                                                                        YearMonth currentMonth) {
        if (membership.getBillingMode() == BillingMode.PACKAGE) {
            return ParticipantMembershipBillingView.forPackage(membership.getProject());
        }

        SubscriptionPaymentStatus status = membershipBillingCalculator.resolveSubscriptionStatus(
                membership.getPaidUntilMonth(), currentMonth);
        Optional<PartialPaymentInfo> partialPaymentInfo = membershipBillingCalculator.resolvePartialPaymentInfo(
                membership.getProject(),
                membership.getPartialPaymentAmount(),
                membership.getPartialPaymentCurrency());

        return ParticipantMembershipBillingView.forSubscription(
                membership.getProject(),
                membership.getPaidUntilMonth(),
                status,
                displayPartialPaymentInfo(status, partialPaymentInfo.orElse(null)),
                resolveCurrentMonthPayment(membership, currentMonth));
    }

    private CurrentMonthPaymentInfo resolveCurrentMonthPayment(ProjectMembership membership,
                                                                 YearMonth currentMonth) {
        if (membership.getBillingMode() == BillingMode.PACKAGE) {
            return CurrentMonthPaymentInfo.notApplicable();
        }

        return switch (membershipBillingCalculator.resolveCurrentMonthPaymentStatus(
                membership.getPaidUntilMonth(), currentMonth)) {
            case PAID -> CurrentMonthPaymentInfo.paid();
            case NOT_PAID -> CurrentMonthPaymentInfo.notPaid();
            case DEBT -> CurrentMonthPaymentInfo.debt(formatDebtAmount(membership));
        };
    }

    private String formatDebtAmount(ProjectMembership membership) {
        Optional<PartialPaymentInfo> partialPaymentInfo = membershipBillingCalculator.resolvePartialPaymentInfo(
                membership.getProject(),
                membership.getPartialPaymentAmount(),
                membership.getPartialPaymentCurrency());
        if (partialPaymentInfo.isPresent()) {
            PartialPaymentInfo partial = partialPaymentInfo.get();
            if (partial.advanceCurrency() == PaymentCurrency.EUR) {
                return reportFormatter.formatEur(partial.remainingUntilRenewal());
            }
            return reportFormatter.formatRub(partial.remainingUntilRenewal());
        }
        return reportFormatter.formatRub(membership.getProject().getMonthlyFeeRub());
    }

    private static PartialPaymentInfo displayPartialPaymentInfo(SubscriptionPaymentStatus status,
                                                                  PartialPaymentInfo partialPaymentInfo) {
        if (status == SubscriptionPaymentStatus.ACTIVE) {
            return null;
        }
        return partialPaymentInfo;
    }
}
