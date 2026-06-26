package com.eliteseriespay.payment.history;

import com.eliteseriespay.domain.BillingMode;
import com.eliteseriespay.service.ProjectService;
import com.eliteseriespay.domain.Participant;
import com.eliteseriespay.domain.Payment;
import com.eliteseriespay.domain.ProjectMembership;
import com.eliteseriespay.repository.PaymentRepository;
import com.eliteseriespay.repository.ProjectMembershipRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectPaymentHistoryService {

    private final PaymentRepository paymentRepository;
    private final ProjectMembershipRepository projectMembershipRepository;
    private final ProjectService projectService;
    private final PaymentHistoryFormatter paymentHistoryFormatter;

    public ProjectPaymentHistoryService(PaymentRepository paymentRepository,
                                        ProjectMembershipRepository projectMembershipRepository,
                                        ProjectService projectService,
                                        PaymentHistoryFormatter paymentHistoryFormatter) {
        this.paymentRepository = paymentRepository;
        this.projectMembershipRepository = projectMembershipRepository;
        this.projectService = projectService;
        this.paymentHistoryFormatter = paymentHistoryFormatter;
    }

    @Transactional(readOnly = true)
    public Page<PaymentHistoryRowView> findProjectPaymentHistory(Long projectId,
                                                                 ProjectPaymentHistoryFilter filter) {
        projectService.findById(projectId);
        Long participantId = resolveParticipantFilter(projectId, filter.participantId());
        Pageable pageable = PageRequest.of(
                filter.page(),
                filter.pageSize(),
                Sort.by(Sort.Order.desc("paymentDate"), Sort.Order.desc("id")));

        Page<Payment> payments = paymentRepository.findProjectPaymentHistory(
                projectId,
                participantId,
                filter.billingMode(),
                filter.source(),
                filter.currency(),
                filter.status(),
                filter.dateFrom(),
                filter.dateTo(),
                pageable);

        Map<Long, BillingMode> billingModes = resolveBillingModes(projectId, payments.getContent());
        List<PaymentHistoryRowView> rows = payments.getContent().stream()
                .map(payment -> paymentHistoryFormatter.toRowView(
                        payment, billingModes.get(payment.getParticipant().getId())))
                .toList();

        return new PageImpl<>(rows, pageable, payments.getTotalElements());
    }

    @Transactional(readOnly = true)
    public boolean hasAnyPayments(Long projectId) {
        projectService.findById(projectId);
        return paymentRepository.existsByProject_Id(projectId);
    }

    @Transactional(readOnly = true)
    public List<Participant> findParticipantsInProjectPaymentHistory(Long projectId) {
        projectService.findById(projectId);
        return paymentRepository.findDistinctParticipantsByProjectIdOrderByNameAsc(projectId);
    }

    private Long resolveParticipantFilter(Long projectId, Long participantId) {
        if (participantId == null) {
            return null;
        }
        return findParticipantsInProjectPaymentHistory(projectId).stream()
                .map(Participant::getId)
                .filter(participantId::equals)
                .findFirst()
                .orElse(null);
    }

    private Map<Long, BillingMode> resolveBillingModes(Long projectId, List<Payment> payments) {
        List<Long> participantIds = payments.stream()
                .map(payment -> payment.getParticipant().getId())
                .distinct()
                .toList();

        if (participantIds.isEmpty()) {
            return Map.of();
        }

        return participantIds.stream()
                .map(participantId -> projectMembershipRepository
                        .findByProject_IdAndParticipant_Id(projectId, participantId)
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        membership -> membership.getParticipant().getId(),
                        ProjectMembership::getBillingMode,
                        (left, right) -> left));
    }
}
