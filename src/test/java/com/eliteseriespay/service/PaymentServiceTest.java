package com.eliteseriespay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eliteseriespay.domain.MembershipStatus;
import com.eliteseriespay.domain.Participant;
import com.eliteseriespay.domain.Payment;
import com.eliteseriespay.domain.PaymentCurrency;
import com.eliteseriespay.domain.PaymentSource;
import com.eliteseriespay.domain.Project;
import com.eliteseriespay.domain.ProjectMembership;
import com.eliteseriespay.exception.NotFoundException;
import com.eliteseriespay.exception.ValidationException;
import com.eliteseriespay.repository.ParticipantRepository;
import com.eliteseriespay.repository.PaymentRepository;
import com.eliteseriespay.repository.ProjectMembershipRepository;
import com.eliteseriespay.repository.ProjectRepository;
import com.eliteseriespay.support.TestEntities;
import com.eliteseriespay.validation.ValidationError;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final long PROJECT_ID = 1L;
    private static final long PARTICIPANT_ID = 10L;
    private static final LocalDate PAYMENT_DATE = LocalDate.of(2026, 6, 21);

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private ProjectMembershipRepository projectMembershipRepository;

    private PaymentService paymentService;

    private Project project;
    private Participant participant;
    private ProjectMembership activeMembership;

    @BeforeEach
    void setUp() {
        ProjectService projectService = new ProjectService(projectRepository);
        ParticipantService participantService = new ParticipantService(participantRepository);
        ProjectMembershipService projectMembershipService = new ProjectMembershipService(
                projectService, participantService, projectMembershipRepository);
        paymentService = new PaymentService(
                paymentRepository, participantService, projectService, projectMembershipService);

        project = TestEntities.project(PROJECT_ID, "Series", new BigDecimal("1000.00"));
        participant = TestEntities.participant(PARTICIPANT_ID, "12345", "Ivan", null);
        activeMembership = new ProjectMembership(project, participant, MembershipStatus.ACTIVE);
    }

    @Test
    void create_calculatesAmountRubForRubCurrency() {
        stubActiveMembership();
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = paymentService.create(
                PARTICIPANT_ID, PROJECT_ID, PAYMENT_DATE, PaymentSource.OTHER,
                new BigDecimal("500.00"), PaymentCurrency.RUB, null, null);

        assertThat(payment.getExchangeRate()).isEqualByComparingTo("1.0000");
        assertThat(payment.getAmountRub()).isEqualByComparingTo("500.00");
        assertThat(payment.getFeePercent()).isZero();
        assertThat(payment.getNetAmountRub()).isEqualByComparingTo("500.00");
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void create_calculatesAmountRubWithExchangeRate() {
        stubActiveMembership();
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = paymentService.create(
                PARTICIPANT_ID, PROJECT_ID, PAYMENT_DATE, PaymentSource.OTHER,
                new BigDecimal("10.00"), PaymentCurrency.USD, new BigDecimal("90.5"), null);

        assertThat(payment.getExchangeRate()).isEqualByComparingTo("90.5000");
        assertThat(payment.getAmountRub()).isEqualByComparingTo("905.00");
    }

    @Test
    void create_appliesVkDonutFee() {
        stubActiveMembership();
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = paymentService.create(
                PARTICIPANT_ID, PROJECT_ID, PAYMENT_DATE, PaymentSource.VK_DONUT,
                new BigDecimal("1000.00"), PaymentCurrency.RUB, null, null);

        assertThat(payment.getFeePercent()).isEqualTo(10);
        assertThat(payment.getNetAmountRub()).isEqualByComparingTo("900.00");
    }

    @Test
    void create_otherSourceHasZeroFee() {
        stubActiveMembership();
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = paymentService.create(
                PARTICIPANT_ID, PROJECT_ID, PAYMENT_DATE, PaymentSource.OTHER,
                new BigDecimal("1000.00"), PaymentCurrency.RUB, null, null);

        assertThat(payment.getFeePercent()).isZero();
        assertThat(payment.getNetAmountRub()).isEqualByComparingTo("1000.00");
    }

    @Test
    void create_rejectsWhenMembershipNotActive() {
        when(participantRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant));
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(projectMembershipRepository.findByProject_IdAndParticipant_Id(PROJECT_ID, PARTICIPANT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.create(
                PARTICIPANT_ID, PROJECT_ID, PAYMENT_DATE, PaymentSource.OTHER,
                new BigDecimal("100.00"), PaymentCurrency.RUB, null, null))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getError())
                        .isEqualTo(ValidationError.NOT_AN_ACTIVE_MEMBER));
    }

    @Test
    void create_succeedsWhenMembershipActive() {
        stubActiveMembership();
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = paymentService.create(
                PARTICIPANT_ID, PROJECT_ID, PAYMENT_DATE, PaymentSource.OTHER,
                new BigDecimal("100.00"), PaymentCurrency.RUB, null, "note");

        assertThat(payment.getParticipant()).isEqualTo(participant);
        assertThat(payment.getProject()).isEqualTo(project);
        assertThat(payment.getComment()).isEqualTo("note");
    }

    @Test
    void create_normalizesExchangeRateToOneForRub() {
        stubActiveMembership();
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = paymentService.create(
                PARTICIPANT_ID, PROJECT_ID, PAYMENT_DATE, PaymentSource.OTHER,
                new BigDecimal("100.00"), PaymentCurrency.RUB, new BigDecimal("99.99"), null);

        assertThat(payment.getExchangeRate()).isEqualByComparingTo("1.0000");
        assertThat(payment.getAmountRub()).isEqualByComparingTo("100.00");
    }

    @Test
    void create_trimsCommentToNull() {
        stubActiveMembership();
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = paymentService.create(
                PARTICIPANT_ID, PROJECT_ID, PAYMENT_DATE, PaymentSource.OTHER,
                new BigDecimal("100.00"), PaymentCurrency.RUB, null, "   ");

        assertThat(payment.getComment()).isNull();
    }

    @Test
    void create_rejectsNullAmount() {
        stubActiveMembership();

        assertThatThrownBy(() -> paymentService.create(
                PARTICIPANT_ID, PROJECT_ID, PAYMENT_DATE, PaymentSource.OTHER,
                null, PaymentCurrency.RUB, null, null))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getError())
                        .isEqualTo(ValidationError.PAYMENT_AMOUNT_REQUIRED));
    }

    @Test
    void create_rejectsNonPositiveAmount() {
        stubActiveMembership();

        assertThatThrownBy(() -> paymentService.create(
                PARTICIPANT_ID, PROJECT_ID, PAYMENT_DATE, PaymentSource.OTHER,
                BigDecimal.ZERO, PaymentCurrency.RUB, null, null))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getError())
                        .isEqualTo(ValidationError.PAYMENT_AMOUNT_NOT_POSITIVE));
    }

    @Test
    void create_rejectsMissingExchangeRateForForeignCurrency() {
        stubActiveMembership();

        assertThatThrownBy(() -> paymentService.create(
                PARTICIPANT_ID, PROJECT_ID, PAYMENT_DATE, PaymentSource.OTHER,
                new BigDecimal("10.00"), PaymentCurrency.USD, null, null))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getError())
                        .isEqualTo(ValidationError.EXCHANGE_RATE_REQUIRED));
    }

    @Test
    void create_rejectsNonPositiveExchangeRate() {
        stubActiveMembership();

        assertThatThrownBy(() -> paymentService.create(
                PARTICIPANT_ID, PROJECT_ID, PAYMENT_DATE, PaymentSource.OTHER,
                new BigDecimal("10.00"), PaymentCurrency.EUR, BigDecimal.ZERO, null))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getError())
                        .isEqualTo(ValidationError.EXCHANGE_RATE_NOT_POSITIVE));
    }

    @Test
    void findByParticipantId_returnsPaymentsNewestFirst() {
        Payment older = TestEntities.payment(
                1L, participant, project, LocalDate.of(2026, 1, 1), PaymentSource.OTHER,
                new BigDecimal("100.00"), PaymentCurrency.RUB, new BigDecimal("1.0000"),
                new BigDecimal("100.00"), 0, new BigDecimal("100.00"), null);
        Payment newer = TestEntities.payment(
                2L, participant, project, LocalDate.of(2026, 6, 1), PaymentSource.OTHER,
                new BigDecimal("200.00"), PaymentCurrency.RUB, new BigDecimal("1.0000"),
                new BigDecimal("200.00"), 0, new BigDecimal("200.00"), null);
        when(participantRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant));
        when(paymentRepository.findByParticipantIdOrderByPaymentDateDescIdDesc(PARTICIPANT_ID))
                .thenReturn(List.of(newer, older));

        List<Payment> payments = paymentService.findByParticipantId(PARTICIPANT_ID);

        assertThat(payments).containsExactly(newer, older);
    }

    @Test
    void findByParticipantId_throwsWhenParticipantNotFound() {
        when(participantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.findByParticipantId(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Participant not found: 99");
    }

    @Test
    void getParticipantPaymentSummary_returnsLatestAndTotal() {
        Payment older = TestEntities.payment(
                1L, participant, project, LocalDate.of(2026, 1, 1), PaymentSource.OTHER,
                new BigDecimal("100.00"), PaymentCurrency.RUB, new BigDecimal("1.0000"),
                new BigDecimal("100.00"), 0, new BigDecimal("100.00"), null);
        Payment newer = TestEntities.payment(
                2L, participant, project, LocalDate.of(2026, 6, 1), PaymentSource.OTHER,
                new BigDecimal("200.00"), PaymentCurrency.RUB, new BigDecimal("1.0000"),
                new BigDecimal("200.00"), 0, new BigDecimal("200.00"), null);
        when(participantRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant));
        when(paymentRepository.findTopByParticipantIdOrderByPaymentDateDescIdDesc(PARTICIPANT_ID))
                .thenReturn(Optional.of(newer));
        when(paymentRepository.sumNetAmountRubByParticipantId(PARTICIPANT_ID))
                .thenReturn(new BigDecimal("300.00"));

        ParticipantPaymentSummary summary = paymentService.getParticipantPaymentSummary(PARTICIPANT_ID);

        assertThat(summary.hasPayments()).isTrue();
        assertThat(summary.latestPaymentDate()).isEqualTo(newer.getPaymentDate());
        assertThat(summary.latestNetAmountRub()).isEqualByComparingTo("200.00");
        assertThat(summary.totalNetAmountRub()).isEqualByComparingTo("300.00");
    }

    @Test
    void getParticipantPaymentSummary_returnsEmptyWhenNoPayments() {
        when(participantRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant));
        when(paymentRepository.findTopByParticipantIdOrderByPaymentDateDescIdDesc(PARTICIPANT_ID))
                .thenReturn(Optional.empty());
        when(paymentRepository.sumNetAmountRubByParticipantId(PARTICIPANT_ID))
                .thenReturn(BigDecimal.ZERO);

        ParticipantPaymentSummary summary = paymentService.getParticipantPaymentSummary(PARTICIPANT_ID);

        assertThat(summary.hasPayments()).isFalse();
        assertThat(summary.latestPaymentDate()).isNull();
        assertThat(summary.latestNetAmountRub()).isNull();
        assertThat(summary.totalNetAmountRub()).isEqualByComparingTo("0");
    }

    @Test
    void findLatestPaymentsByProjectId_returnsLatestPaymentPerParticipant() {
        Participant otherParticipant = TestEntities.participant(11L, "67890", "Anna", null);
        Payment latestForParticipant = TestEntities.payment(
                2L, participant, project, LocalDate.of(2026, 6, 1), PaymentSource.OTHER,
                new BigDecimal("200.00"), PaymentCurrency.RUB, new BigDecimal("1.0000"),
                new BigDecimal("200.00"), 0, new BigDecimal("200.00"), null);
        Payment latestForOtherParticipant = TestEntities.payment(
                3L, otherParticipant, project, LocalDate.of(2026, 3, 1), PaymentSource.OTHER,
                new BigDecimal("150.00"), PaymentCurrency.RUB, new BigDecimal("1.0000"),
                new BigDecimal("150.00"), 0, new BigDecimal("150.00"), null);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(paymentRepository.findLatestPaymentsByProjectId(PROJECT_ID))
                .thenReturn(List.of(latestForParticipant, latestForOtherParticipant));

        Map<Long, Payment> latestPayments = paymentService.findLatestPaymentsByProjectId(PROJECT_ID);

        assertThat(latestPayments).containsOnlyKeys(PARTICIPANT_ID, 11L);
        assertThat(latestPayments.get(PARTICIPANT_ID)).isEqualTo(latestForParticipant);
        assertThat(latestPayments.get(11L)).isEqualTo(latestForOtherParticipant);
    }

    private void stubActiveMembership() {
        when(participantRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant));
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(projectMembershipRepository.findByProject_IdAndParticipant_Id(PROJECT_ID, PARTICIPANT_ID))
                .thenReturn(Optional.of(activeMembership));
    }
}
