package com.eliteseriespay.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.eliteseriespay.domain.Participant;
import com.eliteseriespay.domain.Payment;
import com.eliteseriespay.domain.PaymentCurrency;
import com.eliteseriespay.domain.PaymentSource;
import com.eliteseriespay.domain.Project;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:file:./target/payment-localdate-test.db?busy_timeout=5000"
})
class PaymentRepositoryLocalDateTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesAndReadsPaymentDateAsIsoText() {
        Project project = projectRepository.save(new Project("Series", new BigDecimal("1000.00")));
        Participant participant = participantRepository.save(new Participant("12345", "Ivan", null));
        LocalDate paymentDate = LocalDate.of(2026, 6, 21);

        Payment payment = new Payment(
                participant,
                project,
                paymentDate,
                PaymentSource.OTHER,
                new BigDecimal("500.00"),
                PaymentCurrency.RUB,
                new BigDecimal("1.0000"),
                new BigDecimal("500.00"),
                0,
                new BigDecimal("500.00"),
                null);
        paymentRepository.save(payment);
        entityManager.flush();
        entityManager.clear();

        String storedValue = entityManager.createNativeQuery(
                        "SELECT payment_date FROM payments WHERE id = :id")
                .setParameter("id", payment.getId())
                .getSingleResult()
                .toString();
        assertThat(storedValue).isEqualTo("2026-06-21");

        Payment loaded = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(loaded.getPaymentDate()).isEqualTo(paymentDate);
    }
}
