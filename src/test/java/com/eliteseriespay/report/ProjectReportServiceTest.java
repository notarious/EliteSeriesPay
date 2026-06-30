package com.eliteseriespay.report;

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
import java.time.YearMonth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:file:./target/project-report-service-test.db?busy_timeout=5000"
})
class ProjectReportServiceTest {

    private static final YearMonth REPORT_MONTH = YearMonth.of(2026, 6);
    private static final BigDecimal EPISODE_COST = new BigDecimal("4500.00");

    @Autowired
    private ProjectReportService projectReportService;

    @Autowired
    private com.eliteseriespay.repository.PaymentRepository paymentRepository;

    @Autowired
    private com.eliteseriespay.repository.ParticipantRepository participantRepository;

    @Autowired
    private com.eliteseriespay.repository.ProjectRepository projectRepository;

    @Autowired
    private com.eliteseriespay.repository.ProjectMembershipRepository projectMembershipRepository;

    private Project project;
    private Participant subscriptionParticipant;
    private Participant packageParticipant;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        projectMembershipRepository.deleteAll();
        participantRepository.deleteAll();
        projectRepository.deleteAll();

        project = projectRepository.save(
                new Project("Series", EPISODE_COST, new BigDecimal("500.00"), BigDecimal.ONE));
        subscriptionParticipant = participantRepository.save(new Participant("111", "Ivan", null));
        packageParticipant = participantRepository.save(new Participant("222", "Petr", null));

        projectMembershipRepository.save(new ProjectMembership(
                project, subscriptionParticipant, MembershipStatus.ACTIVE, BillingMode.SUBSCRIPTION));
        projectMembershipRepository.save(new ProjectMembership(
                project, packageParticipant, MembershipStatus.ACTIVE, BillingMode.PACKAGE));
    }

    @Test
    void buildMonthlyReport_singlePaymentMonth() {
        savePayment(subscriptionParticipant, LocalDate.of(2026, 6, 10),
                PaymentSource.MANUAL, PaymentStatus.ACTIVE, PaymentCurrency.RUB,
                new BigDecimal("4500.00"), new BigDecimal("4500.00"), new BigDecimal("4500.00"), 0);

        ProjectMonthlyReport report = projectReportService.buildMonthlyReport(project.getId(), REPORT_MONTH);

        assertThat(report.hasPayments()).isTrue();
        assertThat(report.totals().paymentCount()).isEqualTo(1);
        assertThat(report.totals().totalNetRub()).isEqualByComparingTo("4500.00");
        assertThat(report.episodeFunding().episodesFunded()).isEqualTo(1);
        assertThat(report.episodeFunding().remainingRub()).isEqualByComparingTo("0.00");
    }

    @Test
    void buildMonthlyReport_mixedCurrenciesAndBillingModes() {
        savePayment(subscriptionParticipant, LocalDate.of(2026, 6, 5),
                PaymentSource.VK_DONUT, PaymentStatus.ACTIVE, PaymentCurrency.RUB,
                new BigDecimal("10000.00"), new BigDecimal("10000.00"), new BigDecimal("9000.00"), 10);
        savePayment(packageParticipant, LocalDate.of(2026, 6, 12),
                PaymentSource.MANUAL, PaymentStatus.ACTIVE, PaymentCurrency.EUR,
                new BigDecimal("100.00"), new BigDecimal("9000.00"), new BigDecimal("9000.00"), 0);
        savePayment(subscriptionParticipant, LocalDate.of(2026, 6, 20),
                PaymentSource.MANUAL, PaymentStatus.ACTIVE, PaymentCurrency.USD,
                new BigDecimal("50.00"), new BigDecimal("4500.00"), new BigDecimal("4500.00"), 0);

        ProjectMonthlyReport report = projectReportService.buildMonthlyReport(project.getId(), REPORT_MONTH);

        assertThat(report.totals().totalGrossRub()).isEqualByComparingTo("23500.00");
        assertThat(report.totals().totalNetRub()).isEqualByComparingTo("22500.00");
        assertThat(report.episodeFunding().episodesFunded()).isEqualTo(5);
        assertThat(report.episodeFunding().remainingRub()).isEqualByComparingTo("0.00");
        assertThat(report.participantContributions()).hasSize(2);
    }

    @Test
    void buildMonthlyReport_excludesVoidedPayments() {
        savePayment(subscriptionParticipant, LocalDate.of(2026, 6, 1),
                PaymentSource.MANUAL, PaymentStatus.ACTIVE, PaymentCurrency.RUB,
                new BigDecimal("9000.00"), new BigDecimal("9000.00"), new BigDecimal("9000.00"), 0);
        savePayment(subscriptionParticipant, LocalDate.of(2026, 6, 2),
                PaymentSource.MANUAL, PaymentStatus.VOIDED, PaymentCurrency.RUB,
                new BigDecimal("4500.00"), new BigDecimal("4500.00"), new BigDecimal("4500.00"), 0);
        savePayment(subscriptionParticipant, LocalDate.of(2026, 5, 31),
                PaymentSource.MANUAL, PaymentStatus.ACTIVE, PaymentCurrency.RUB,
                new BigDecimal("4500.00"), new BigDecimal("4500.00"), new BigDecimal("4500.00"), 0);

        ProjectMonthlyReport report = projectReportService.buildMonthlyReport(project.getId(), REPORT_MONTH);

        assertThat(report.totals().paymentCount()).isEqualTo(1);
        assertThat(report.totals().totalNetRub()).isEqualByComparingTo("9000.00");
    }

    @Test
    void buildMonthlyReport_emptyMonth() {
        ProjectMonthlyReport report = projectReportService.buildMonthlyReport(project.getId(), REPORT_MONTH);

        assertThat(report.hasPayments()).isFalse();
        assertThat(report.totals().paymentCount()).isZero();
    }

    @Test
    void buildMonthlyReport_vkDonutBreakdownUsesNetAmount() {
        savePayment(subscriptionParticipant, LocalDate.of(2026, 6, 1),
                PaymentSource.VK_DONUT, PaymentStatus.ACTIVE, PaymentCurrency.RUB,
                new BigDecimal("1000.00"), new BigDecimal("1000.00"), new BigDecimal("900.00"), 10);

        ProjectMonthlyReport report = projectReportService.buildMonthlyReport(project.getId(), REPORT_MONTH);

        SourceBreakdownRow vkRow = report.sourceBreakdown().stream()
                .filter(row -> row.source() == PaymentSource.VK_DONUT)
                .findFirst()
                .orElseThrow();
        assertThat(vkRow.paymentCount()).isEqualTo(1);
        assertThat(vkRow.grossAmountRub()).contains("1");
        assertThat(vkRow.netAmountRub()).contains("900");
    }

    @Test
    void buildMonthlyReport_episodeCalculationWithRemainder() {
        savePayment(subscriptionParticipant, LocalDate.of(2026, 6, 1),
                PaymentSource.MANUAL, PaymentStatus.ACTIVE, PaymentCurrency.RUB,
                new BigDecimal("31800.00"), new BigDecimal("31800.00"), new BigDecimal("31800.00"), 0);

        ProjectMonthlyReport report = projectReportService.buildMonthlyReport(project.getId(), REPORT_MONTH);

        assertThat(report.episodeFunding().episodesFunded()).isEqualTo(7);
        assertThat(report.episodeFunding().remainingRub()).isEqualByComparingTo("300.00");
        assertThat(report.episodeFunding().amountToNextEpisode()).isEqualByComparingTo("4200.00");
    }

    @Test
    void buildCurrentMonthSummary_usesSameCalculationAsReport() {
        YearMonth currentMonth = YearMonth.now();
        savePayment(subscriptionParticipant, currentMonth.atDay(1),
                PaymentSource.MANUAL, PaymentStatus.ACTIVE, PaymentCurrency.RUB,
                new BigDecimal("31800.00"), new BigDecimal("31800.00"), new BigDecimal("31800.00"), 0);

        ProjectMonthlyReport report = projectReportService.buildMonthlyReport(project.getId(), currentMonth);
        ProjectMonthlySummaryView summary = projectReportService.buildCurrentMonthSummary(project.getId());

        assertThat(summary.hasPayments()).isEqualTo(report.hasPayments());
        assertThat(summary.totalNetRubFormatted()).isEqualTo(report.totalNetRubFormatted());
        assertThat(summary.episodeCostRubFormatted()).isEqualTo(report.episodeCostRubFormatted());
        assertThat(summary.episodesFundedLine()).isEqualTo(ReportText.episodesFundedLine(
                report.episodeFunding().episodesFunded()));
        assertThat(summary.remainingLine()).isEqualTo("Остаток: " + report.remainingRubFormatted() + ".");
    }

    @Test
    void buildCurrentMonthSummary_usesBillingPeriodTitle() {
        ProjectMonthlySummaryView summary = projectReportService.buildCurrentMonthSummary(project.getId());

        YearMonth currentMonth = YearMonth.now();
        String expectedTitle = new ReportFormatter().formatBillingCollectionSummaryTitle(currentMonth);
        assertThat(summary.monthLabel()).isEqualTo(expectedTitle);
    }

    private void savePayment(Participant participant,
                             LocalDate paymentDate,
                             PaymentSource source,
                             PaymentStatus status,
                             PaymentCurrency currency,
                             BigDecimal amountOriginal,
                             BigDecimal amountRub,
                             BigDecimal netAmountRub,
                             int feePercent) {
        BigDecimal exchangeRate = currency == PaymentCurrency.RUB
                ? new BigDecimal("1.0000")
                : new BigDecimal("90.0000");
        Payment payment = new Payment(
                participant,
                project,
                paymentDate,
                source,
                amountOriginal,
                currency,
                exchangeRate,
                amountRub,
                feePercent,
                netAmountRub,
                null,
                status);
        paymentRepository.save(payment);
    }
}
