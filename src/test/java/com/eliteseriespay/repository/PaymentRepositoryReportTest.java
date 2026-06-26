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
import com.eliteseriespay.report.MonthlyPaymentAggregate;
import com.eliteseriespay.report.ParticipantNetAggregate;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:file:./target/project-report-repo-test.db?busy_timeout=5000"
})
class PaymentRepositoryReportTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMembershipRepository projectMembershipRepository;

    private Project project;
    private Participant participant;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        projectMembershipRepository.deleteAll();
        participantRepository.deleteAll();
        projectRepository.deleteAll();

        participant = participantRepository.save(new Participant("111", "Ivan", null));
        project = projectRepository.save(
                new Project("Series", new BigDecimal("4500.00"), new BigDecimal("500.00"), BigDecimal.ONE));
        projectMembershipRepository.save(new ProjectMembership(
                project, participant, MembershipStatus.ACTIVE, BillingMode.SUBSCRIPTION));
    }

    @Test
    void sumActivePaymentsByProjectAndDateRange_excludesVoidedAndOutsideRange() {
        savePayment(LocalDate.of(2026, 6, 10), PaymentSource.MANUAL, PaymentStatus.ACTIVE,
                new BigDecimal("1000.00"), new BigDecimal("900.00"));
        savePayment(LocalDate.of(2026, 6, 15), PaymentSource.VK_DONUT, PaymentStatus.VOIDED,
                new BigDecimal("500.00"), new BigDecimal("450.00"));
        savePayment(LocalDate.of(2026, 5, 31), PaymentSource.MANUAL, PaymentStatus.ACTIVE,
                new BigDecimal("200.00"), new BigDecimal("200.00"));

        MonthlyPaymentAggregate totals = paymentRepository.sumActivePaymentsByProjectAndDateRange(
                project.getId(), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertThat(totals.totalGrossRub()).isEqualByComparingTo("1000.00");
        assertThat(totals.totalNetRub()).isEqualByComparingTo("900.00");
        assertThat(totals.paymentCount()).isEqualTo(1L);
    }

    @Test
    void sumNetByParticipantForProjectAndDateRange_groupsByParticipantAndBillingMode() {
        Participant packageParticipant = participantRepository.save(new Participant("222", "Petr", null));
        projectMembershipRepository.save(new ProjectMembership(
                project, packageParticipant, MembershipStatus.ACTIVE, BillingMode.PACKAGE));

        savePayment(participant, LocalDate.of(2026, 6, 1), PaymentSource.MANUAL, PaymentStatus.ACTIVE,
                new BigDecimal("1000.00"), new BigDecimal("1000.00"));
        savePayment(packageParticipant, LocalDate.of(2026, 6, 2), PaymentSource.MANUAL, PaymentStatus.ACTIVE,
                new BigDecimal("500.00"), new BigDecimal("500.00"));

        var rows = paymentRepository.sumNetByParticipantForProjectAndDateRange(
                project.getId(), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertThat(rows).hasSize(2);
        assertThat(rows.stream().map(ParticipantNetAggregate::participantName).toList())
                .containsExactly("Ivan", "Petr");
    }

    private void savePayment(LocalDate date, PaymentSource source, PaymentStatus status,
                             BigDecimal amountRub, BigDecimal netAmountRub) {
        savePayment(participant, date, source, status, amountRub, netAmountRub);
    }

    private void savePayment(Participant paymentParticipant, LocalDate date, PaymentSource source,
                             PaymentStatus status, BigDecimal amountRub, BigDecimal netAmountRub) {
        int feePercent = source == PaymentSource.VK_DONUT ? 10 : 0;
        Payment payment = new Payment(
                paymentParticipant,
                project,
                date,
                source,
                amountRub,
                PaymentCurrency.RUB,
                new BigDecimal("1.0000"),
                amountRub,
                feePercent,
                netAmountRub,
                null,
                status);
        paymentRepository.save(payment);
    }
}
