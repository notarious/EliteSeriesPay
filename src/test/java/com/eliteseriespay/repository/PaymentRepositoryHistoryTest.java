package com.eliteseriespay.repository;

import static org.assertj.core.api.Assertions.assertThat;

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
        "spring.datasource.url=jdbc:sqlite:file:./target/payment-history-test.db?busy_timeout=5000"
})
class PaymentRepositoryHistoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMembershipRepository projectMembershipRepository;

    private Participant participant;
    private Project projectA;
    private Project projectB;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        projectMembershipRepository.deleteAll();
        participantRepository.deleteAll();
        projectRepository.deleteAll();

        participant = participantRepository.save(new Participant("12345", "Ivan", null));
        projectA = projectRepository.save(new Project("Series A", new BigDecimal("1000.00")));
        projectB = projectRepository.save(new Project("Series B", new BigDecimal("1000.00")));
        projectMembershipRepository.save(new ProjectMembership(projectA, participant, MembershipStatus.ACTIVE));
        projectMembershipRepository.save(new ProjectMembership(projectB, participant, MembershipStatus.ACTIVE));
    }

    @Test
    void findParticipantPaymentHistory_paginatesAndSortsByDateAndIdDesc() {
        Payment older = savePayment(projectA, LocalDate.of(2026, 1, 1), PaymentSource.MANUAL, PaymentStatus.ACTIVE, "100.00");
        Payment middle = savePayment(projectA, LocalDate.of(2026, 6, 1), PaymentSource.MANUAL, PaymentStatus.ACTIVE, "200.00");
        Payment newest = savePayment(projectA, LocalDate.of(2026, 6, 1), PaymentSource.MANUAL, PaymentStatus.ACTIVE, "250.00");

        Page<Payment> firstPage = paymentRepository.findParticipantPaymentHistory(
                participant.getId(), null, null, null, null, null,
                PageRequest.of(0, 2, Sort.by(Sort.Order.desc("paymentDate"), Sort.Order.desc("id"))));

        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getContent()).extracting(Payment::getId)
                .containsExactly(newest.getId(), middle.getId());

        Page<Payment> secondPage = paymentRepository.findParticipantPaymentHistory(
                participant.getId(), null, null, null, null, null,
                PageRequest.of(1, 2, Sort.by(Sort.Order.desc("paymentDate"), Sort.Order.desc("id"))));

        assertThat(secondPage.getContent()).extracting(Payment::getId).containsExactly(older.getId());
        assertThat(secondPage.getContent().getFirst().getProject().getName()).isEqualTo("Series A");
    }

    @Test
    void findParticipantPaymentHistory_filtersByProjectSourceStatusAndDateRange() {
        savePayment(projectA, LocalDate.of(2026, 1, 15), PaymentSource.VK_DONUT, PaymentStatus.ACTIVE, "100.00");
        Payment target = savePayment(projectB, LocalDate.of(2026, 3, 10), PaymentSource.MANUAL, PaymentStatus.ACTIVE, "200.00");
        savePayment(projectB, LocalDate.of(2026, 5, 1), PaymentSource.MANUAL, PaymentStatus.VOIDED, "300.00");
        savePayment(projectB, LocalDate.of(2026, 7, 1), PaymentSource.MANUAL, PaymentStatus.ACTIVE, "400.00");

        Page<Payment> page = paymentRepository.findParticipantPaymentHistory(
                participant.getId(),
                projectB.getId(),
                PaymentSource.MANUAL,
                PaymentStatus.ACTIVE,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 6, 1),
                PageRequest.of(0, 25, Sort.by(Sort.Order.desc("paymentDate"), Sort.Order.desc("id"))));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).extracting(Payment::getId).containsExactly(target.getId());
    }

    @Test
    void findDistinctProjectsByParticipantIdOrderByNameAsc_returnsOnlyProjectsFromPayments() {
        savePayment(projectB, LocalDate.of(2026, 3, 10), PaymentSource.MANUAL, PaymentStatus.ACTIVE, "200.00");
        savePayment(projectA, LocalDate.of(2026, 1, 15), PaymentSource.MANUAL, PaymentStatus.ACTIVE, "100.00");

        assertThat(paymentRepository.findDistinctProjectsByParticipantIdOrderByNameAsc(participant.getId()))
                .extracting(Project::getName)
                .containsExactly("Series A", "Series B");
    }

    @Test
    void findDistinctProjectsByParticipantIdOrderByNameAsc_excludesProjectsWithoutPayments() {
        assertThat(paymentRepository.findDistinctProjectsByParticipantIdOrderByNameAsc(participant.getId()))
                .isEmpty();
    }

    @Test
    void findParticipantPaymentHistory_returnsEmptyPageWhenNothingMatches() {
        savePayment(projectA, LocalDate.of(2026, 1, 1), PaymentSource.MANUAL, PaymentStatus.ACTIVE, "100.00");

        Page<Payment> page = paymentRepository.findParticipantPaymentHistory(
                participant.getId(),
                projectB.getId(),
                null,
                null,
                null,
                null,
                PageRequest.of(0, 25, Sort.by(Sort.Order.desc("paymentDate"), Sort.Order.desc("id"))));

        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getContent()).isEmpty();
    }

    private Payment savePayment(Project project,
                                LocalDate paymentDate,
                                PaymentSource source,
                                PaymentStatus status,
                                String netAmountRub) {
        BigDecimal amount = new BigDecimal(netAmountRub);
        int feePercent = source == PaymentSource.VK_DONUT ? 10 : 0;
        BigDecimal amountRub = amount;
        BigDecimal net = source == PaymentSource.VK_DONUT
                ? amount.multiply(new BigDecimal("0.9")).setScale(2, java.math.RoundingMode.HALF_UP)
                : amount;
        Payment payment = new Payment(
                participant,
                project,
                paymentDate,
                source,
                amount,
                PaymentCurrency.RUB,
                new BigDecimal("1.0000"),
                amountRub,
                feePercent,
                net,
                null,
                status);
        return paymentRepository.save(payment);
    }
}
