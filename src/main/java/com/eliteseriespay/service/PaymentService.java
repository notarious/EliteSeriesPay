package com.eliteseriespay.service;

import com.eliteseriespay.domain.Participant;
import com.eliteseriespay.domain.Payment;
import com.eliteseriespay.domain.PaymentCurrency;
import com.eliteseriespay.domain.PaymentSource;
import com.eliteseriespay.domain.Project;
import com.eliteseriespay.domain.ProjectMembership;
import com.eliteseriespay.repository.PaymentRepository;
import com.eliteseriespay.util.Texts;
import com.eliteseriespay.validation.PaymentValidator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private static final int VK_DONUT_FEE_PERCENT = 10;
    private static final int MONEY_SCALE = 2;
    private static final int EXCHANGE_RATE_SCALE = 4;

    private final PaymentRepository paymentRepository;
    private final ParticipantService participantService;
    private final ProjectService projectService;
    private final ProjectMembershipService projectMembershipService;

    public PaymentService(PaymentRepository paymentRepository,
                          ParticipantService participantService,
                          ProjectService projectService,
                          ProjectMembershipService projectMembershipService) {
        this.paymentRepository = paymentRepository;
        this.participantService = participantService;
        this.projectService = projectService;
        this.projectMembershipService = projectMembershipService;
    }

    @Transactional(readOnly = true)
    public List<Payment> findByParticipantId(Long participantId) {
        participantService.findById(participantId);
        return paymentRepository.findByParticipantIdOrderByPaymentDateDescIdDesc(participantId);
    }

    @Transactional(readOnly = true)
    public ParticipantPaymentSummary getParticipantPaymentSummary(Long participantId) {
        participantService.findById(participantId);
        BigDecimal totalNetAmountRub = paymentRepository.sumNetAmountRubByParticipantId(participantId);
        return paymentRepository.findTopByParticipantIdOrderByPaymentDateDescIdDesc(participantId)
                .map(payment -> new ParticipantPaymentSummary(
                        payment.getPaymentDate(),
                        payment.getNetAmountRub(),
                        totalNetAmountRub))
                .orElseGet(() -> new ParticipantPaymentSummary(null, null, totalNetAmountRub));
    }

    @Transactional(readOnly = true)
    public Map<Long, Payment> findLatestPaymentsByProjectId(Long projectId) {
        projectService.findById(projectId);
        return paymentRepository.findLatestPaymentsByProjectId(projectId).stream()
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
        BigDecimal normalizedAmount = normalizeAmount(amountOriginal);
        BigDecimal normalizedExchangeRate = normalizeExchangeRate(currency, exchangeRate);

        PaymentValidator.validate(paymentDate, source, normalizedAmount, currency, normalizedExchangeRate);

        BigDecimal amountRub = calculateAmountRub(normalizedAmount, normalizedExchangeRate);
        int feePercent = calculateFeePercent(source);
        BigDecimal netAmountRub = calculateNetAmountRub(amountRub, feePercent);

        Payment payment = new Payment(
                participant,
                project,
                paymentDate,
                source,
                normalizedAmount,
                currency,
                normalizedExchangeRate,
                amountRub,
                feePercent,
                netAmountRub,
                normalizedComment);

        return paymentRepository.save(payment);
    }

    private BigDecimal normalizeAmount(BigDecimal amountOriginal) {
        if (amountOriginal == null) {
            return null;
        }
        return amountOriginal.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeExchangeRate(PaymentCurrency currency, BigDecimal exchangeRate) {
        if (currency == PaymentCurrency.RUB) {
            return BigDecimal.ONE.setScale(EXCHANGE_RATE_SCALE, RoundingMode.HALF_UP);
        }
        if (exchangeRate == null) {
            return null;
        }
        return exchangeRate.setScale(EXCHANGE_RATE_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAmountRub(BigDecimal amountOriginal, BigDecimal exchangeRate) {
        return amountOriginal.multiply(exchangeRate).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private int calculateFeePercent(PaymentSource source) {
        return source == PaymentSource.VK_DONUT ? VK_DONUT_FEE_PERCENT : 0;
    }

    private BigDecimal calculateNetAmountRub(BigDecimal amountRub, int feePercent) {
        return amountRub
                .multiply(BigDecimal.valueOf(100 - feePercent))
                .divide(BigDecimal.valueOf(100), MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
