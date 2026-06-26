package com.eliteseriespay.service;

import com.eliteseriespay.billing.MembershipBillingService;
import com.eliteseriespay.billing.ParticipantPaymentSummary;
import com.eliteseriespay.domain.Participant;
import com.eliteseriespay.domain.Payment;
import com.eliteseriespay.payment.NormalizedAmounts;
import com.eliteseriespay.payment.PaymentAmounts;
import com.eliteseriespay.payment.PaymentCalculator;
import com.eliteseriespay.payment.PaymentFormDefaults;
import com.eliteseriespay.payment.history.ParticipantPaymentHistoryFilter;
import com.eliteseriespay.domain.PaymentCurrency;
import com.eliteseriespay.domain.PaymentSource;
import com.eliteseriespay.domain.PaymentStatus;
import com.eliteseriespay.domain.Project;
import com.eliteseriespay.exception.NotFoundException;
import com.eliteseriespay.exception.ValidationException;
import com.eliteseriespay.repository.PaymentRepository;
import com.eliteseriespay.util.Texts;
import com.eliteseriespay.validation.PaymentValidator;
import com.eliteseriespay.validation.ValidationError;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ParticipantService participantService;
    private final ProjectService projectService;
    private final ProjectMembershipService projectMembershipService;
    private final PaymentCalculator paymentCalculator;
    private final ApplicationSettingsService applicationSettingsService;
    private final MembershipBillingService membershipBillingService;

    public PaymentService(PaymentRepository paymentRepository,
                          ParticipantService participantService,
                          ProjectService projectService,
                          ProjectMembershipService projectMembershipService,
                          PaymentCalculator paymentCalculator,
                          ApplicationSettingsService applicationSettingsService,
                          MembershipBillingService membershipBillingService) {
        this.paymentRepository = paymentRepository;
        this.participantService = participantService;
        this.projectService = projectService;
        this.projectMembershipService = projectMembershipService;
        this.paymentCalculator = paymentCalculator;
        this.applicationSettingsService = applicationSettingsService;
        this.membershipBillingService = membershipBillingService;
    }

    @Transactional(readOnly = true)
    public List<Payment> findByParticipantId(Long participantId) {
        participantService.findById(participantId);
        return paymentRepository.findByParticipantIdOrderByPaymentDateDescIdDesc(participantId);
    }

    @Transactional(readOnly = true)
    public Page<Payment> findParticipantPaymentHistory(Long participantId,
                                                       ParticipantPaymentHistoryFilter filter) {
        participantService.findById(participantId);
        Long projectId = resolveProjectFilter(participantId, filter.projectId());
        Pageable pageable = PageRequest.of(
                filter.page(),
                filter.pageSize(),
                Sort.by(Sort.Order.desc("paymentDate"), Sort.Order.desc("id")));

        return paymentRepository.findParticipantPaymentHistory(
                participantId,
                projectId,
                filter.source(),
                filter.status(),
                filter.dateFrom(),
                filter.dateTo(),
                pageable);
    }

    @Transactional(readOnly = true)
    public boolean hasAnyPayments(Long participantId) {
        participantService.findById(participantId);
        return paymentRepository.existsByParticipant_Id(participantId);
    }

    @Transactional(readOnly = true)
    public List<Project> findProjectsInParticipantPaymentHistory(Long participantId) {
        participantService.findById(participantId);
        return paymentRepository.findDistinctProjectsByParticipantIdOrderByNameAsc(participantId);
    }

    @Transactional(readOnly = true)
    public Payment findById(Long participantId, Long paymentId) {
        participantService.findById(participantId);
        return paymentRepository.findByIdAndParticipantId(paymentId, participantId)
                .orElseThrow(() -> new NotFoundException("Payment", paymentId));
    }

    @Transactional(readOnly = true)
    public ParticipantPaymentSummary getParticipantPaymentSummary(Long participantId) {
        participantService.findById(participantId);
        BigDecimal totalNetAmountRub = paymentRepository.sumActiveNetAmountRubByParticipantId(participantId);
        return paymentRepository.findFirstByParticipant_IdAndStatusOrderByPaymentDateDescIdDesc(
                        participantId, PaymentStatus.ACTIVE)
                .map(payment -> new ParticipantPaymentSummary(
                        payment.getPaymentDate(),
                        payment.getProject().getName(),
                        payment.getAmountRub(),
                        payment.getNetAmountRub(),
                        totalNetAmountRub))
                .orElseGet(() -> new ParticipantPaymentSummary(null, null, null, null, totalNetAmountRub));
    }

    @Transactional(readOnly = true)
    public Map<Long, Payment> findLatestPaymentsByProjectId(Long projectId) {
        projectService.findById(projectId);
        return paymentRepository.findLatestActivePaymentsByProjectId(projectId).stream()
                .collect(Collectors.toMap(payment -> payment.getParticipant().getId(), Function.identity()));
    }

    @Transactional(readOnly = true)
    public PaymentFormDefaults getNewPaymentFormDefaults(Long participantId, Set<Long> activeProjectIds) {
        participantService.findById(participantId);

        LocalDate paymentDate = LocalDate.now();
        return paymentRepository.findFirstByParticipant_IdAndStatusOrderByPaymentDateDescIdDesc(
                        participantId, PaymentStatus.ACTIVE)
                .map(payment -> buildDefaultsFromLatestActivePayment(payment, activeProjectIds, paymentDate))
                .orElseGet(() -> emptyPaymentFormDefaults(paymentDate));
    }

    @Transactional
    public Payment create(Long participantId,
                          Long projectId,
                          LocalDate paymentDate,
                          PaymentSource source,
                          BigDecimal amountOriginal,
                          PaymentCurrency currency,
                          BigDecimal exchangeRate,
                          String comment) {
        Participant participant = participantService.findById(participantId);
        Project project = projectService.findById(projectId);
        projectMembershipService.getActiveMembership(projectId, participantId);

        String normalizedComment = Texts.trimToNull(comment);
        NormalizedAmounts normalized = paymentCalculator.normalize(amountOriginal, currency, exchangeRate);
        PaymentValidator.validate(
                paymentDate, source, normalized.amountOriginal(), currency, normalized.exchangeRate());
        PaymentAmounts amounts = paymentCalculator.calculate(
                source, normalized, applicationSettingsService.getVkDonutFeePercent());

        Payment payment = new Payment(
                participant,
                project,
                paymentDate,
                source,
                amounts.amountOriginal(),
                currency,
                amounts.exchangeRate(),
                amounts.amountRub(),
                amounts.feePercent(),
                amounts.netAmountRub(),
                normalizedComment);

        Payment saved = paymentRepository.save(payment);
        membershipBillingService.recalculateBilling(projectId, participantId);
        return saved;
    }

    @Transactional
    public Payment createInitialMembershipPayment(Long participantId,
                                                  Long projectId,
                                                  LocalDate paymentDate,
                                                  PaymentSource source,
                                                  BigDecimal amountOriginal,
                                                  PaymentCurrency currency,
                                                  BigDecimal exchangeRate,
                                                  String comment) {
        participantService.findById(participantId);
        projectService.findById(projectId);

        String normalizedComment = Texts.trimToNull(comment);
        NormalizedAmounts normalized = paymentCalculator.normalize(amountOriginal, currency, exchangeRate);
        PaymentValidator.validate(
                paymentDate, source, normalized.amountOriginal(), currency, normalized.exchangeRate());
        PaymentAmounts amounts = paymentCalculator.calculate(
                source, normalized, applicationSettingsService.getVkDonutFeePercent());

        projectMembershipService.completeSubscriptionAdd(projectId, participantId);

        Participant participant = participantService.findById(participantId);
        Project project = projectService.findById(projectId);

        Payment payment = new Payment(
                participant,
                project,
                paymentDate,
                source,
                amounts.amountOriginal(),
                currency,
                amounts.exchangeRate(),
                amounts.amountRub(),
                amounts.feePercent(),
                amounts.netAmountRub(),
                normalizedComment);

        Payment saved = paymentRepository.save(payment);
        membershipBillingService.recalculateBilling(projectId, participantId);
        return saved;
    }

    @Transactional
    public Payment update(Long participantId,
                          Long paymentId,
                          Long projectId,
                          LocalDate paymentDate,
                          PaymentSource source,
                          BigDecimal amountOriginal,
                          PaymentCurrency currency,
                          BigDecimal exchangeRate,
                          String comment) {
        Payment payment = findById(participantId, paymentId);
        ensurePaymentActive(payment);

        Project project = projectService.findById(projectId);
        projectMembershipService.getActiveMembership(projectId, participantId);

        String normalizedComment = Texts.trimToNull(comment);
        NormalizedAmounts normalized = paymentCalculator.normalize(amountOriginal, currency, exchangeRate);
        PaymentValidator.validate(
                paymentDate, source, normalized.amountOriginal(), currency, normalized.exchangeRate());
        PaymentAmounts amounts = paymentCalculator.calculate(
                source, normalized, applicationSettingsService.getVkDonutFeePercent());

        payment.update(
                project,
                paymentDate,
                source,
                amounts.amountOriginal(),
                currency,
                amounts.exchangeRate(),
                amounts.amountRub(),
                amounts.feePercent(),
                amounts.netAmountRub(),
                normalizedComment);

        membershipBillingService.recalculateBilling(projectId, participantId);
        return payment;
    }

    @Transactional
    public void voidPayment(Long participantId, Long paymentId) {
        Payment payment = findById(participantId, paymentId);
        if (payment.getStatus() == PaymentStatus.VOIDED) {
            throw new ValidationException(ValidationError.PAYMENT_ALREADY_VOIDED);
        }
        payment.voidPayment();
        membershipBillingService.recalculateBilling(payment.getProject().getId(), participantId);
    }

    private void ensurePaymentActive(Payment payment) {
        if (payment.getStatus() == PaymentStatus.VOIDED) {
            throw new ValidationException(ValidationError.PAYMENT_VOIDED);
        }
    }

    private PaymentFormDefaults buildDefaultsFromLatestActivePayment(Payment payment,
                                                                     Set<Long> activeProjectIds,
                                                                     LocalDate paymentDate) {
        Long projectId = activeProjectIds.contains(payment.getProject().getId())
                ? payment.getProject().getId()
                : null;
        BigDecimal exchangeRate = payment.getCurrency() == PaymentCurrency.RUB
                ? null
                : payment.getExchangeRate();

        return new PaymentFormDefaults(
                paymentDate,
                projectId,
                payment.getSource(),
                payment.getAmountOriginal(),
                payment.getCurrency(),
                exchangeRate);
    }

    private PaymentFormDefaults emptyPaymentFormDefaults(LocalDate paymentDate) {
        return new PaymentFormDefaults(paymentDate, null, null, null, null, null);
    }

    private Long resolveProjectFilter(Long participantId, Long projectId) {
        if (projectId == null) {
            return null;
        }
        return findProjectsInParticipantPaymentHistory(participantId).stream()
                .map(Project::getId)
                .filter(projectId::equals)
                .findFirst()
                .orElse(null);
    }
}
