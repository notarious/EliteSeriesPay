package com.eliteseriespay.service;

import com.eliteseriespay.domain.Participant;
import com.eliteseriespay.domain.Payment;
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
import java.util.function.Function;
import java.util.stream.Collectors;
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

    public PaymentService(PaymentRepository paymentRepository,
                          ParticipantService participantService,
                          ProjectService projectService,
                          ProjectMembershipService projectMembershipService,
                          PaymentCalculator paymentCalculator,
                          ApplicationSettingsService applicationSettingsService) {
        this.paymentRepository = paymentRepository;
        this.participantService = participantService;
        this.projectService = projectService;
        this.projectMembershipService = projectMembershipService;
        this.paymentCalculator = paymentCalculator;
        this.applicationSettingsService = applicationSettingsService;
    }

    @Transactional(readOnly = true)
    public List<Payment> findByParticipantId(Long participantId) {
        participantService.findById(participantId);
        return paymentRepository.findByParticipantIdOrderByPaymentDateDescIdDesc(participantId);
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

        return paymentRepository.save(payment);
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

        return payment;
    }

    @Transactional
    public void voidPayment(Long participantId, Long paymentId) {
        Payment payment = findById(participantId, paymentId);
        if (payment.getStatus() == PaymentStatus.VOIDED) {
            throw new ValidationException(ValidationError.PAYMENT_ALREADY_VOIDED);
        }
        payment.voidPayment();
    }

    private void ensurePaymentActive(Payment payment) {
        if (payment.getStatus() == PaymentStatus.VOIDED) {
            throw new ValidationException(ValidationError.PAYMENT_VOIDED);
        }
    }
}
