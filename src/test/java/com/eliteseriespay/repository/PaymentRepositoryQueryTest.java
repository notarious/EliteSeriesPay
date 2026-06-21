package com.eliteseriespay.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.eliteseriespay.domain.Participant;
import com.eliteseriespay.domain.Payment;
import com.eliteseriespay.domain.PaymentCurrency;
import com.eliteseriespay.domain.PaymentSource;
import com.eliteseriespay.domain.Project;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:file:./target/payment-query-test.db?busy_timeout=5000"
})
class PaymentRepositoryQueryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    void findByParticipantIdOrderByPaymentDateDescIdDesc_sortsNewestFirst() {
        Project project = projectRepository.save(new Project("Series", new BigDecimal("1000.00")));
        Participant participant = participantRepository.save(new Participant("12345", "Ivan", null));

        Payment older = savePayment(participant, project, LocalDate.of(2026, 1, 1), "100.00");
        Payment newerSameDateHigherId = savePayment(participant, project, LocalDate.of(2026, 6, 1), "200.00");
        Payment newest = savePayment(participant, project, LocalDate.of(2026, 6, 1), "250.00");

        List<Payment> payments = paymentRepository.findByParticipantIdOrderByPaymentDateDescIdDesc(participant.getId());

        assertThat(payments).containsExactly(newest, newerSameDateHigherId, older);
    }

    @Test
    void findTopByParticipantIdAndProjectIdOrderByPaymentDateDescIdDesc_returnsLatestForProject() {
        Project projectA = projectRepository.save(new Project("Series A", new BigDecimal("1000.00")));
        Project projectB = projectRepository.save(new Project("Series B", new BigDecimal("1000.00")));
        Participant participant = participantRepository.save(new Participant("12345", "Ivan", null));

        savePayment(participant, projectA, LocalDate.of(2026, 1, 1), "100.00");
        Payment latestInProjectA = savePayment(participant, projectA, LocalDate.of(2026, 6, 1), "200.00");
        savePayment(participant, projectB, LocalDate.of(2026, 12, 1), "300.00");

        Payment latest = paymentRepository
                .findTopByParticipantIdAndProjectIdOrderByPaymentDateDescIdDesc(participant.getId(), projectA.getId())
                .orElseThrow();

        assertThat(latest.getId()).isEqualTo(latestInProjectA.getId());
        assertThat(latest.getNetAmountRub()).isEqualByComparingTo("200.00");
    }

    @Test
    void sumNetAmountRubByParticipantId_returnsTotalNetAmount() {
        Project project = projectRepository.save(new Project("Series", new BigDecimal("1000.00")));
        Participant participant = participantRepository.save(new Participant("12345", "Ivan", null));

        savePayment(participant, project, LocalDate.of(2026, 1, 1), "100.00");
        savePayment(participant, project, LocalDate.of(2026, 2, 1), "200.00");

        BigDecimal total = paymentRepository.sumNetAmountRubByParticipantId(participant.getId());

        assertThat(total).isEqualByComparingTo("300.00");
    }

    @Test
    void findLatestPaymentsByProjectId_returnsLatestPaymentPerParticipant() {
        Project project = projectRepository.save(new Project("Series", new BigDecimal("1000.00")));
        Participant ivan = participantRepository.save(new Participant("12345", "Ivan", null));
        Participant anna = participantRepository.save(new Participant("67890", "Anna", null));

        savePayment(ivan, project, LocalDate.of(2026, 1, 1), "100.00");
        Payment latestIvan = savePayment(ivan, project, LocalDate.of(2026, 6, 1), "200.00");
        Payment latestAnna = savePayment(anna, project, LocalDate.of(2026, 3, 1), "150.00");

        List<Payment> latestPayments = paymentRepository.findLatestPaymentsByProjectId(project.getId());

        assertThat(latestPayments).extracting(Payment::getId)
                .containsExactlyInAnyOrder(latestIvan.getId(), latestAnna.getId());
    }

    private Payment savePayment(Participant participant,
                                Project project,
                                LocalDate paymentDate,
                                String netAmountRub) {
        BigDecimal amount = new BigDecimal(netAmountRub);
        BigDecimal netAmount = new BigDecimal(netAmountRub);
        Payment payment = new Payment(
                participant,
                project,
                paymentDate,
                PaymentSource.OTHER,
                amount,
                PaymentCurrency.RUB,
                new BigDecimal("1.0000"),
                amount,
                0,
                netAmount,
                null);
        return paymentRepository.save(payment);
    }
}
