package com.eliteseriespay.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.eliteseriespay.domain.BillingMode;
import com.eliteseriespay.domain.MembershipStatus;
import com.eliteseriespay.domain.Participant;
import com.eliteseriespay.domain.Payment;
import com.eliteseriespay.domain.PaymentCurrency;
import com.eliteseriespay.domain.PaymentSource;
import com.eliteseriespay.domain.PaymentStatus;
import com.eliteseriespay.domain.Project;
import com.eliteseriespay.domain.ProjectMembership;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:file:./target/project-payment-history-test.db?busy_timeout=5000"
})
class PaymentRepositoryProjectHistoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMembershipRepository projectMembershipRepository;

    private Participant subscriptionParticipant;
    private Participant packageParticipant;
    private Project projectA;
    private Project projectB;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        projectMembershipRepository.deleteAll();
        participantRepository.deleteAll();
        projectRepository.deleteAll();

        subscriptionParticipant = participantRepository.save(new Participant("111", "Ivan", null));
        packageParticipant = participantRepository.save(new Participant("222", "Petr", null));
        projectA = projectRepository.save(
                new Project("Series A", new BigDecimal("1000.00"), new BigDecimal("500.00"), BigDecimal.ONE));
        projectB = projectRepository.save(
                new Project("Series B", new BigDecimal("1000.00"), new BigDecimal("500.00"), BigDecimal.ONE));

        projectMembershipRepository.save(new ProjectMembership(
                projectA, subscriptionParticipant, MembershipStatus.ACTIVE, BillingMode.SUBSCRIPTION));
        projectMembershipRepository.save(new ProjectMembership(
                projectA, packageParticipant, MembershipStatus.ACTIVE, BillingMode.PACKAGE));
        projectMembershipRepository.save(new ProjectMembership(
                projectB, subscriptionParticipant, MembershipStatus.ACTIVE, BillingMode.SUBSCRIPTION));
    }

    @Test
    void findProjectPaymentHistory_returnsOnlyPaymentsFromSelectedProject() {
        Payment projectAPayment = savePayment(projectA, subscriptionParticipant,
                LocalDate.of(2026, 6, 1), PaymentSource.MANUAL, PaymentStatus.ACTIVE,
                PaymentCurrency.RUB, "100.00");
        savePayment(projectB, subscriptionParticipant,
                LocalDate.of(2026, 6, 2), PaymentSource.MANUAL, PaymentStatus.ACTIVE,
                PaymentCurrency.RUB, "200.00");

        Page<Payment> page = paymentRepository.findProjectPaymentHistory(
                projectA.getId(), null, null, null, null, null, null, null,
                PageRequest.of(0, 25, Sort.by(Sort.Order.desc("paymentDate"), Sort.Order.desc("id"))));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).extracting(Payment::getId).containsExactly(projectAPayment.getId());
    }

    @Test
    void findProjectPaymentHistory_filtersByParticipantBillingModeSourceCurrencyStatusAndDateRange() {
        savePayment(projectA, subscriptionParticipant,
                LocalDate.of(2026, 1, 15), PaymentSource.VK_DONUT, PaymentStatus.ACTIVE,
                PaymentCurrency.RUB, "100.00");
        Payment target = savePayment(projectA, packageParticipant,
                LocalDate.of(2026, 3, 10), PaymentSource.MANUAL, PaymentStatus.ACTIVE,
                PaymentCurrency.EUR, "200.00");
        savePayment(projectA, packageParticipant,
                LocalDate.of(2026, 5, 1), PaymentSource.MANUAL, PaymentStatus.VOIDED,
                PaymentCurrency.EUR, "300.00");
        savePayment(projectA, packageParticipant,
                LocalDate.of(2026, 7, 1), PaymentSource.MANUAL, PaymentStatus.ACTIVE,
                PaymentCurrency.EUR, "400.00");

        Page<Payment> page = paymentRepository.findProjectPaymentHistory(
                projectA.getId(),
                packageParticipant.getId(),
                BillingMode.PACKAGE,
                PaymentSource.MANUAL,
                PaymentCurrency.EUR,
                PaymentStatus.ACTIVE,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 6, 1),
                PageRequest.of(0, 25, Sort.by(Sort.Order.desc("paymentDate"), Sort.Order.desc("id"))));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).extracting(Payment::getId).containsExactly(target.getId());
    }

    @Test
    void findProjectPaymentHistory_paginatesAndSortsByDateAndIdDesc() {
        Payment older = savePayment(projectA, subscriptionParticipant,
                LocalDate.of(2026, 1, 1), PaymentSource.MANUAL, PaymentStatus.ACTIVE,
                PaymentCurrency.RUB, "100.00");
        Payment middle = savePayment(projectA, subscriptionParticipant,
                LocalDate.of(2026, 6, 1), PaymentSource.MANUAL, PaymentStatus.ACTIVE,
                PaymentCurrency.RUB, "200.00");
        Payment newest = savePayment(projectA, subscriptionParticipant,
                LocalDate.of(2026, 6, 1), PaymentSource.MANUAL, PaymentStatus.ACTIVE,
                PaymentCurrency.RUB, "250.00");

        Page<Payment> firstPage = paymentRepository.findProjectPaymentHistory(
                projectA.getId(), null, null, null, null, null, null, null,
                PageRequest.of(0, 2, Sort.by(Sort.Order.desc("paymentDate"), Sort.Order.desc("id"))));

        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getContent()).extracting(Payment::getId)
                .containsExactly(newest.getId(), middle.getId());

        Page<Payment> secondPage = paymentRepository.findProjectPaymentHistory(
                projectA.getId(), null, null, null, null, null, null, null,
                PageRequest.of(1, 2, Sort.by(Sort.Order.desc("paymentDate"), Sort.Order.desc("id"))));

        assertThat(secondPage.getContent()).extracting(Payment::getId).containsExactly(older.getId());
    }

    @Test
    void findDistinctParticipantsByProjectIdOrderByNameAsc_returnsParticipantsWithPayments() {
        savePayment(projectA, subscriptionParticipant,
                LocalDate.of(2026, 1, 1), PaymentSource.MANUAL, PaymentStatus.ACTIVE,
                PaymentCurrency.RUB, "100.00");
        savePayment(projectA, packageParticipant,
                LocalDate.of(2026, 2, 1), PaymentSource.MANUAL, PaymentStatus.ACTIVE,
                PaymentCurrency.RUB, "100.00");

        assertThat(paymentRepository.findDistinctParticipantsByProjectIdOrderByNameAsc(projectA.getId()))
                .extracting(Participant::getName)
                .containsExactly("Ivan", "Petr");
    }

    private Payment savePayment(Project project,
                                Participant participant,
                                LocalDate paymentDate,
                                PaymentSource source,
                                PaymentStatus status,
                                PaymentCurrency currency,
                                String amount) {
        BigDecimal value = new BigDecimal(amount);
        int feePercent = source == PaymentSource.VK_DONUT ? 10 : 0;
        BigDecimal net = source == PaymentSource.VK_DONUT
                ? value.multiply(new BigDecimal("0.9")).setScale(2, java.math.RoundingMode.HALF_UP)
                : value;
        BigDecimal exchangeRate = currency == PaymentCurrency.RUB
                ? new BigDecimal("1.0000")
                : new BigDecimal("90.0000");
        BigDecimal amountRub = currency == PaymentCurrency.RUB ? value : value.multiply(exchangeRate);

        Payment payment = new Payment(
                participant,
                project,
                paymentDate,
                source,
                value,
                currency,
                exchangeRate,
                amountRub,
                feePercent,
                net,
                null,
                status);
        return paymentRepository.save(payment);
    }
}
