package com.eliteseriespay.service;

import com.eliteseriespay.domain.BillingMode;
import com.eliteseriespay.domain.Payment;
import com.eliteseriespay.domain.ProjectMembership;
import com.eliteseriespay.domain.SubscriptionPaymentStatus;
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

    public MembershipBillingService(ProjectMembershipRepository projectMembershipRepository,
                                    PaymentRepository paymentRepository,
                                    MembershipBillingCalculator membershipBillingCalculator) {
        this.projectMembershipRepository = projectMembershipRepository;
        this.paymentRepository = paymentRepository;
        this.membershipBillingCalculator = membershipBillingCalculator;
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
    public List<ProjectParticipantBillingView> buildProjectParticipantViews(
            List<ProjectMembership> memberships,
            BillingModeFilter billingModeFilter,
            SubscriptionPaymentStatusFilter paymentStatusFilter,
            YearMonth currentMonth) {

        return memberships.stream()
                .filter(membership -> membershipBillingCalculator.matchesBillingMode(
                        membership.getBillingMode(), billingModeFilter))
                .map(membership -> toBillingView(membership, currentMonth))
                .filter(view -> matchesPaymentStatusFilter(view, paymentStatusFilter))
                .toList();
    }

    private ProjectParticipantBillingView toBillingView(ProjectMembership membership, YearMonth currentMonth) {
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
                partialPaymentInfo.orElse(null));
    }

    private boolean matchesPaymentStatusFilter(ProjectParticipantBillingView view,
                                               SubscriptionPaymentStatusFilter filter) {
        if (filter.isAll()) {
            return true;
        }
        return membershipBillingCalculator.matchesPaymentStatus(
                view.billingMode(), view.subscriptionPaymentStatus(), filter);
    }
}
