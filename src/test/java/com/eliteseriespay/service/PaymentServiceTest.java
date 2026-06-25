package com.eliteseriespay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eliteseriespay.domain.ApplicationSettings;
import com.eliteseriespay.domain.MembershipStatus;
import com.eliteseriespay.domain.Participant;
import com.eliteseriespay.domain.Payment;
import com.eliteseriespay.domain.PaymentCurrency;
import com.eliteseriespay.domain.PaymentSource;
import com.eliteseriespay.domain.PaymentStatus;
import com.eliteseriespay.domain.Project;
import com.eliteseriespay.domain.ProjectMembership;
import com.eliteseriespay.exception.NotFoundException;
import com.eliteseriespay.exception.ValidationException;
import com.eliteseriespay.repository.ApplicationSettingsRepository;
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
import java.util.Set;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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

    @Mock
    private ApplicationSettingsRepository applicationSettingsRepository;

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
        ApplicationSettingsService applicationSettingsService =
                new ApplicationSettingsService(applicationSettingsRepository);
        paymentService = new PaymentService(
                paymentRepository, participantService, projectService, projectMembershipService,
                new PaymentCalculator(), applicationSettingsService);

        project = TestEntities.project(PROJECT_ID, "Series", new BigDecimal("1000.00"));
        participant = TestEntities.participant(PARTICIPANT_ID, "12345", "Ivan", null);
        activeMembership = new ProjectMembership(project, participant, MembershipStatus.ACTIVE);
    }

    @Test
    void create_usesCurrentVkDonutFeePercentFromSettings() {
        stubActiveMembership();
        when(applicationSettingsRepository.findById(ApplicationSettings.SINGLETON_ID))
                .thenReturn(Optional.of(new ApplicationSettings(15)));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = paymentService.create(
                PARTICIPANT_ID, PROJECT_ID, PAYMENT_DATE, PaymentSource.VK_DONUT,
                new BigDecimal("1000.00"), PaymentCurrency.RUB, null, null);

        assertThat(payment.getFeePercent()).isEqualTo(15);
        assertThat(payment.getNetAmountRub()).isEqualByComparingTo("850.00");
    }

    @Test
    void create_calculatesAmountRubForRubCurrency() {
        stubActiveMembership();
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = paymentService.create(
                PARTICIPANT_ID, PROJECT_ID, PAYMENT_DATE, PaymentSource.MANUAL,
                new BigDecimal("500.00"), PaymentCurrency.RUB, null, null);

        assertThat(payment.getExchangeRate()).isEqualByComparingTo("1.0000");
        assertThat(payment.getAmountRub()).isEqualByComparingTo("500.00");
        assertThat(payment.getFeePercent()).isZero();
        assertThat(payment.getNetAmountRub()).isEqualByComparingTo("500.00");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.ACTIVE);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void create_calculatesAmountRubWithExchangeRate() {
        stubActiveMembership();
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = paymentService.create(
                PARTICIPANT_ID, PROJECT_ID, PAYMENT_DATE, PaymentSource.MANUAL,
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
                PARTICIPANT_ID, PROJECT_ID, PAYMENT_DATE, PaymentSource.MANUAL,
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
                PARTICIPANT_ID, PROJECT_ID, PAYMENT_DATE, PaymentSource.MANUAL,
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
                PARTICIPANT_ID, PROJECT_ID, PAYMENT_DATE, PaymentSource.MANUAL,
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
                PARTICIPANT_ID, PROJECT_ID, PAYMENT_DATE, PaymentSource.MANUAL,
                new BigDecimal("100.00"), PaymentCurrency.RUB, new BigDecimal("99.99"), null);

        assertThat(payment.getExchangeRate()).isEqualByComparingTo("1.0000");
        assertThat(payment.getAmountRub()).isEqualByComparingTo("100.00");
    }

    @Test
    void create_trimsCommentToNull() {
        stubActiveMembership();
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = paymentService.create(
                PARTICIPANT_ID, PROJECT_ID, PAYMENT_DATE, PaymentSource.MANUAL,
                new BigDecimal("100.00"), PaymentCurrency.RUB, null, "   ");

        assertThat(payment.getComment()).isNull();
    }

    @Test
    void create_rejectsNullAmount() {
        stubActiveMembership();

        assertThatThrownBy(() -> paymentService.create(
                PARTICIPANT_ID, PROJECT_ID, PAYMENT_DATE, PaymentSource.MANUAL,
                null, PaymentCurrency.RUB, null, null))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getError())
                        .isEqualTo(ValidationError.PAYMENT_AMOUNT_REQUIRED));
    }

    @Test
    void create_rejectsNonPositiveAmount() {
        stubActiveMembership();

        assertThatThrownBy(() -> paymentService.create(
                PARTICIPANT_ID, PROJECT_ID, PAYMENT_DATE, PaymentSource.MANUAL,
                BigDecimal.ZERO, PaymentCurrency.RUB, null, null))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getError())
                        .isEqualTo(ValidationError.PAYMENT_AMOUNT_NOT_POSITIVE));
    }

    @Test
    void create_rejectsMissingExchangeRateForForeignCurrency() {
        stubActiveMembership();

        assertThatThrownBy(() -> paymentService.create(
                PARTICIPANT_ID, PROJECT_ID, PAYMENT_DATE, PaymentSource.MANUAL,
                new BigDecimal("10.00"), PaymentCurrency.USD, null, null))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getError())
                        .isEqualTo(ValidationError.EXCHANGE_RATE_REQUIRED));
    }

    @Test
    void create_rejectsNonPositiveExchangeRate() {
        stubActiveMembership();

        assertThatThrownBy(() -> paymentService.create(
                PARTICIPANT_ID, PROJECT_ID, PAYMENT_DATE, PaymentSource.MANUAL,
                new BigDecimal("10.00"), PaymentCurrency.EUR, BigDecimal.ZERO, null))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getError())
                        .isEqualTo(ValidationError.EXCHANGE_RATE_NOT_POSITIVE));
    }

    @Test
    void findByParticipantId_returnsPaymentsNewestFirst() {
        Payment older = TestEntities.payment(
                1L, participant, project, LocalDate.of(2026, 1, 1), PaymentSource.MANUAL,
                new BigDecimal("100.00"), PaymentCurrency.RUB, new BigDecimal("1.0000"),
                new BigDecimal("100.00"), 0, new BigDecimal("100.00"), null);
        Payment newer = TestEntities.payment(
                2L, participant, project, LocalDate.of(2026, 6, 1), PaymentSource.MANUAL,
                new BigDecimal("200.00"), PaymentCurrency.RUB, new BigDecimal("1.0000"),
                new BigDecimal("200.00"), 0, new BigDecimal("200.00"), null);
        when(participantRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant));
        when(paymentRepository.findByParticipantIdOrderByPaymentDateDescIdDesc(PARTICIPANT_ID))
                .thenReturn(List.of(newer, older));

        List<Payment> payments = paymentService.findByParticipantId(PARTICIPANT_ID);

        assertThat(payments).containsExactly(newer, older);
    }

    @Test
    void findParticipantPaymentHistory_delegatesToRepositoryWithNormalizedFilter() {
        ParticipantPaymentHistoryFilter filter = ParticipantPaymentHistoryFilter.of(
                PROJECT_ID, PaymentSource.MANUAL, PaymentStatus.ACTIVE,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 1), 1, 50);
        Payment payment = TestEntities.payment(
                1L, participant, project, PAYMENT_DATE, PaymentSource.MANUAL,
                new BigDecimal("100.00"), PaymentCurrency.RUB, new BigDecimal("1.0000"),
                new BigDecimal("100.00"), 0, new BigDecimal("100.00"), null);
        Page<Payment> page = new PageImpl<>(List.of(payment), PageRequest.of(1, 50), 2);

        when(participantRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant));
        when(paymentRepository.findDistinctProjectsByParticipantIdOrderByNameAsc(PARTICIPANT_ID))
                .thenReturn(List.of(project));
        when(paymentRepository.findParticipantPaymentHistory(
                eq(PARTICIPANT_ID), eq(PROJECT_ID), eq(PaymentSource.MANUAL), eq(PaymentStatus.ACTIVE),
                eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 6, 1)), any(Pageable.class)))
                .thenReturn(page);

        Page<Payment> result = paymentService.findParticipantPaymentHistory(PARTICIPANT_ID, filter);

        assertThat(result.getContent()).containsExactly(payment);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(paymentRepository).findParticipantPaymentHistory(
                eq(PARTICIPANT_ID), eq(PROJECT_ID), eq(PaymentSource.MANUAL), eq(PaymentStatus.ACTIVE),
                eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 6, 1)), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
        assertThat(pageableCaptor.getValue().getSort()).isEqualTo(
                Sort.by(Sort.Order.desc("paymentDate"), Sort.Order.desc("id")));
    }

    @Test
    void findParticipantPaymentHistory_ignoresUnknownProjectFilter() {
        ParticipantPaymentHistoryFilter filter = ParticipantPaymentHistoryFilter.of(
                99L, null, null, null, null, 0, 25);
        Page<Payment> page = new PageImpl<>(List.of());

        when(participantRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant));
        when(paymentRepository.findDistinctProjectsByParticipantIdOrderByNameAsc(PARTICIPANT_ID))
                .thenReturn(List.of(project));
        when(paymentRepository.findParticipantPaymentHistory(
                eq(PARTICIPANT_ID), eq(null), eq(null), eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(page);

        paymentService.findParticipantPaymentHistory(PARTICIPANT_ID, filter);

        verify(paymentRepository).findParticipantPaymentHistory(
                eq(PARTICIPANT_ID), eq(null), eq(null), eq(null), eq(null), eq(null), any(Pageable.class));
    }

    @Test
    void findProjectsInParticipantPaymentHistory_returnsProjectsFromRepository() {
        when(participantRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant));
        when(paymentRepository.findDistinctProjectsByParticipantIdOrderByNameAsc(PARTICIPANT_ID))
                .thenReturn(List.of(project));

        assertThat(paymentService.findProjectsInParticipantPaymentHistory(PARTICIPANT_ID))
                .containsExactly(project);
    }

    @Test
    void hasAnyPayments_returnsRepositoryResult() {
        when(participantRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant));
        when(paymentRepository.existsByParticipant_Id(PARTICIPANT_ID)).thenReturn(true);

        assertThat(paymentService.hasAnyPayments(PARTICIPANT_ID)).isTrue();
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
                1L, participant, project, LocalDate.of(2026, 1, 1), PaymentSource.MANUAL,
                new BigDecimal("100.00"), PaymentCurrency.RUB, new BigDecimal("1.0000"),
                new BigDecimal("100.00"), 0, new BigDecimal("100.00"), null);
        Payment newer = TestEntities.payment(
                2L, participant, project, LocalDate.of(2026, 6, 1), PaymentSource.MANUAL,
                new BigDecimal("200.00"), PaymentCurrency.RUB, new BigDecimal("1.0000"),
                new BigDecimal("200.00"), 0, new BigDecimal("200.00"), null);
        when(participantRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant));
        when(paymentRepository.findFirstByParticipant_IdAndStatusOrderByPaymentDateDescIdDesc(
                PARTICIPANT_ID, PaymentStatus.ACTIVE))
                .thenReturn(Optional.of(newer));
        when(paymentRepository.sumActiveNetAmountRubByParticipantId(PARTICIPANT_ID))
                .thenReturn(new BigDecimal("300.00"));

        ParticipantPaymentSummary summary = paymentService.getParticipantPaymentSummary(PARTICIPANT_ID);

        assertThat(summary.hasPayments()).isTrue();
        assertThat(summary.latestPaymentDate()).isEqualTo(newer.getPaymentDate());
        assertThat(summary.latestProjectName()).isEqualTo(project.getName());
        assertThat(summary.latestAmountRub()).isEqualByComparingTo("200.00");
        assertThat(summary.latestNetAmountRub()).isEqualByComparingTo("200.00");
        assertThat(summary.totalNetAmountRub()).isEqualByComparingTo("300.00");
    }

    @Test
    void getParticipantPaymentSummary_returnsEmptyWhenNoPayments() {
        when(participantRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant));
        when(paymentRepository.findFirstByParticipant_IdAndStatusOrderByPaymentDateDescIdDesc(
                PARTICIPANT_ID, PaymentStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(paymentRepository.sumActiveNetAmountRubByParticipantId(PARTICIPANT_ID))
                .thenReturn(BigDecimal.ZERO);

        ParticipantPaymentSummary summary = paymentService.getParticipantPaymentSummary(PARTICIPANT_ID);

        assertThat(summary.hasPayments()).isFalse();
        assertThat(summary.latestPaymentDate()).isNull();
        assertThat(summary.latestProjectName()).isNull();
        assertThat(summary.latestAmountRub()).isNull();
        assertThat(summary.latestNetAmountRub()).isNull();
        assertThat(summary.totalNetAmountRub()).isEqualByComparingTo("0");
    }

    @Test
    void findLatestPaymentsByProjectId_returnsLatestPaymentPerParticipant() {
        Participant otherParticipant = TestEntities.participant(11L, "67890", "Anna", null);
        Payment latestForParticipant = TestEntities.payment(
                2L, participant, project, LocalDate.of(2026, 6, 1), PaymentSource.MANUAL,
                new BigDecimal("200.00"), PaymentCurrency.RUB, new BigDecimal("1.0000"),
                new BigDecimal("200.00"), 0, new BigDecimal("200.00"), null);
        Payment latestForOtherParticipant = TestEntities.payment(
                3L, otherParticipant, project, LocalDate.of(2026, 3, 1), PaymentSource.MANUAL,
                new BigDecimal("150.00"), PaymentCurrency.RUB, new BigDecimal("1.0000"),
                new BigDecimal("150.00"), 0, new BigDecimal("150.00"), null);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(paymentRepository.findLatestActivePaymentsByProjectId(PROJECT_ID))
                .thenReturn(List.of(latestForParticipant, latestForOtherParticipant));

        Map<Long, Payment> latestPayments = paymentService.findLatestPaymentsByProjectId(PROJECT_ID);

        assertThat(latestPayments).containsOnlyKeys(PARTICIPANT_ID, 11L);
        assertThat(latestPayments.get(PARTICIPANT_ID)).isEqualTo(latestForParticipant);
        assertThat(latestPayments.get(11L)).isEqualTo(latestForOtherParticipant);
    }

    @Test
    void update_recalculatesAmountsOnSave() {
        Payment existing = TestEntities.payment(
                5L, participant, project, PAYMENT_DATE, PaymentSource.MANUAL,
                new BigDecimal("100.00"), PaymentCurrency.RUB, new BigDecimal("1.0000"),
                new BigDecimal("100.00"), 0, new BigDecimal("100.00"), null);
        when(participantRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant));
        when(paymentRepository.findByIdAndParticipantId(5L, PARTICIPANT_ID))
                .thenReturn(Optional.of(existing));
        stubActiveMembership();

        Payment updated = paymentService.update(
                PARTICIPANT_ID, 5L, PROJECT_ID, PAYMENT_DATE, PaymentSource.VK_DONUT,
                new BigDecimal("200.00"), PaymentCurrency.RUB, null, "corrected");

        assertThat(updated.getAmountRub()).isEqualByComparingTo("200.00");
        assertThat(updated.getFeePercent()).isEqualTo(10);
        assertThat(updated.getNetAmountRub()).isEqualByComparingTo("180.00");
        assertThat(updated.getComment()).isEqualTo("corrected");
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.ACTIVE);
    }

    @Test
    void update_rejectsVoidedPayment() {
        Payment voided = TestEntities.payment(
                5L, participant, project, PAYMENT_DATE, PaymentSource.MANUAL,
                new BigDecimal("100.00"), PaymentCurrency.RUB, new BigDecimal("1.0000"),
                new BigDecimal("100.00"), 0, new BigDecimal("100.00"), null, PaymentStatus.VOIDED);
        when(participantRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant));
        when(paymentRepository.findByIdAndParticipantId(5L, PARTICIPANT_ID))
                .thenReturn(Optional.of(voided));

        assertThatThrownBy(() -> paymentService.update(
                PARTICIPANT_ID, 5L, PROJECT_ID, PAYMENT_DATE, PaymentSource.MANUAL,
                new BigDecimal("100.00"), PaymentCurrency.RUB, null, null))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getError())
                        .isEqualTo(ValidationError.PAYMENT_VOIDED));
    }

    @Test
    void voidPayment_marksPaymentAsVoided() {
        Payment existing = TestEntities.payment(
                5L, participant, project, PAYMENT_DATE, PaymentSource.MANUAL,
                new BigDecimal("100.00"), PaymentCurrency.RUB, new BigDecimal("1.0000"),
                new BigDecimal("100.00"), 0, new BigDecimal("100.00"), null);
        when(participantRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant));
        when(paymentRepository.findByIdAndParticipantId(5L, PARTICIPANT_ID))
                .thenReturn(Optional.of(existing));

        paymentService.voidPayment(PARTICIPANT_ID, 5L);

        assertThat(existing.getStatus()).isEqualTo(PaymentStatus.VOIDED);
    }

    @Test
    void voidPayment_rejectsAlreadyVoided() {
        Payment voided = TestEntities.payment(
                5L, participant, project, PAYMENT_DATE, PaymentSource.MANUAL,
                new BigDecimal("100.00"), PaymentCurrency.RUB, new BigDecimal("1.0000"),
                new BigDecimal("100.00"), 0, new BigDecimal("100.00"), null, PaymentStatus.VOIDED);
        when(participantRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant));
        when(paymentRepository.findByIdAndParticipantId(5L, PARTICIPANT_ID))
                .thenReturn(Optional.of(voided));

        assertThatThrownBy(() -> paymentService.voidPayment(PARTICIPANT_ID, 5L))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getError())
                        .isEqualTo(ValidationError.PAYMENT_ALREADY_VOIDED));
    }

    @Test
    void getParticipantPaymentSummary_excludesVoidedPayments() {
        Payment voidedLatest = TestEntities.payment(
                3L, participant, project, LocalDate.of(2026, 12, 1), PaymentSource.MANUAL,
                new BigDecimal("500.00"), PaymentCurrency.RUB, new BigDecimal("1.0000"),
                new BigDecimal("500.00"), 0, new BigDecimal("500.00"), null, PaymentStatus.VOIDED);
        Payment activeOlder = TestEntities.payment(
                2L, participant, project, LocalDate.of(2026, 6, 1), PaymentSource.MANUAL,
                new BigDecimal("200.00"), PaymentCurrency.RUB, new BigDecimal("1.0000"),
                new BigDecimal("200.00"), 0, new BigDecimal("200.00"), null);
        when(participantRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant));
        when(paymentRepository.findFirstByParticipant_IdAndStatusOrderByPaymentDateDescIdDesc(
                PARTICIPANT_ID, PaymentStatus.ACTIVE))
                .thenReturn(Optional.of(activeOlder));
        when(paymentRepository.sumActiveNetAmountRubByParticipantId(PARTICIPANT_ID))
                .thenReturn(new BigDecimal("200.00"));

        ParticipantPaymentSummary summary = paymentService.getParticipantPaymentSummary(PARTICIPANT_ID);

        assertThat(summary.latestPaymentDate()).isEqualTo(activeOlder.getPaymentDate());
        assertThat(summary.latestAmountRub()).isEqualByComparingTo("200.00");
        assertThat(summary.totalNetAmountRub()).isEqualByComparingTo("200.00");
        assertThat(voidedLatest.getStatus()).isEqualTo(PaymentStatus.VOIDED);
    }

    @Test
    void getNewPaymentFormDefaults_returnsTodayWhenNoActivePayments() {
        when(participantRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant));
        when(paymentRepository.findFirstByParticipant_IdAndStatusOrderByPaymentDateDescIdDesc(
                PARTICIPANT_ID, PaymentStatus.ACTIVE))
                .thenReturn(Optional.empty());

        PaymentFormDefaults defaults = paymentService.getNewPaymentFormDefaults(
                PARTICIPANT_ID, Set.of(PROJECT_ID));

        assertThat(defaults.paymentDate()).isEqualTo(LocalDate.now());
        assertThat(defaults.projectId()).isNull();
        assertThat(defaults.source()).isNull();
        assertThat(defaults.amountOriginal()).isNull();
        assertThat(defaults.currency()).isNull();
        assertThat(defaults.exchangeRate()).isNull();
    }

    @Test
    void getNewPaymentFormDefaults_prefillsFromLatestActivePayment() {
        Payment latestActive = TestEntities.payment(
                2L, participant, project, LocalDate.of(2026, 5, 1), PaymentSource.VK_DONUT,
                new BigDecimal("250.00"), PaymentCurrency.USD, new BigDecimal("90.5000"),
                new BigDecimal("22625.00"), 10, new BigDecimal("20362.50"), "keep me out");
        when(participantRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant));
        when(paymentRepository.findFirstByParticipant_IdAndStatusOrderByPaymentDateDescIdDesc(
                PARTICIPANT_ID, PaymentStatus.ACTIVE))
                .thenReturn(Optional.of(latestActive));

        PaymentFormDefaults defaults = paymentService.getNewPaymentFormDefaults(
                PARTICIPANT_ID, Set.of(PROJECT_ID));

        assertThat(defaults.paymentDate()).isEqualTo(LocalDate.now());
        assertThat(defaults.projectId()).isEqualTo(PROJECT_ID);
        assertThat(defaults.source()).isEqualTo(PaymentSource.VK_DONUT);
        assertThat(defaults.amountOriginal()).isEqualByComparingTo("250.00");
        assertThat(defaults.currency()).isEqualTo(PaymentCurrency.USD);
        assertThat(defaults.exchangeRate()).isEqualByComparingTo("90.5000");
    }

    @Test
    void getNewPaymentFormDefaults_omitsProjectWhenMembershipNotActive() {
        Payment latestActive = TestEntities.payment(
                2L, participant, project, LocalDate.of(2026, 5, 1), PaymentSource.MANUAL,
                new BigDecimal("150.00"), PaymentCurrency.RUB, new BigDecimal("1.0000"),
                new BigDecimal("150.00"), 0, new BigDecimal("150.00"), null);
        when(participantRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant));
        when(paymentRepository.findFirstByParticipant_IdAndStatusOrderByPaymentDateDescIdDesc(
                PARTICIPANT_ID, PaymentStatus.ACTIVE))
                .thenReturn(Optional.of(latestActive));

        PaymentFormDefaults defaults = paymentService.getNewPaymentFormDefaults(
                PARTICIPANT_ID, Set.of(99L));

        assertThat(defaults.projectId()).isNull();
        assertThat(defaults.source()).isEqualTo(PaymentSource.MANUAL);
        assertThat(defaults.amountOriginal()).isEqualByComparingTo("150.00");
        assertThat(defaults.currency()).isEqualTo(PaymentCurrency.RUB);
        assertThat(defaults.exchangeRate()).isNull();
    }

    @Test
    void getNewPaymentFormDefaults_ignoresVoidedPayments() {
        when(participantRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant));
        when(paymentRepository.findFirstByParticipant_IdAndStatusOrderByPaymentDateDescIdDesc(
                PARTICIPANT_ID, PaymentStatus.ACTIVE))
                .thenReturn(Optional.empty());

        PaymentFormDefaults defaults = paymentService.getNewPaymentFormDefaults(
                PARTICIPANT_ID, Set.of(PROJECT_ID));

        assertThat(defaults.projectId()).isNull();
        assertThat(defaults.amountOriginal()).isNull();
    }

    private void stubDefaultSettings() {
        lenient().when(applicationSettingsRepository.findById(ApplicationSettings.SINGLETON_ID))
                .thenReturn(Optional.of(new ApplicationSettings(10)));
    }

    private void stubActiveMembership() {
        stubDefaultSettings();
        when(participantRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant));
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(projectMembershipRepository.findByProject_IdAndParticipant_Id(PROJECT_ID, PARTICIPANT_ID))
                .thenReturn(Optional.of(activeMembership));
    }
}
