package com.eliteseriespay.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProjectMonthlySummaryViewTest {

    @Test
    void from_buildsRemainingAndNextEpisodeLinesWhenRemainderExists() {
        ProjectMonthlyReport report = sampleReport(
                new BigDecimal("31800.00"),
                EpisodeFunding.calculate(new BigDecimal("31800.00"), new BigDecimal("4500.00")));

        ProjectMonthlySummaryView summary = ProjectMonthlySummaryView.from(report);

        assertThat(summary.hasPayments()).isTrue();
        assertThat(summary.episodesFundedLine()).isEqualTo("Собрано на 7 серий.");
        assertThat(summary.remainingLine()).isEqualTo("Остаток: 300,00 ₽.");
        assertThat(summary.amountToNextEpisodeLine()).isEqualTo("До 8-й серии осталось: 4 200,00 ₽.");
    }

    @Test
    void from_buildsNextEpisodeLineWhenRemainderIsZero() {
        ProjectMonthlyReport report = sampleReport(
                new BigDecimal("31500.00"),
                EpisodeFunding.calculate(new BigDecimal("31500.00"), new BigDecimal("4500.00")));

        ProjectMonthlySummaryView summary = ProjectMonthlySummaryView.from(report);

        assertThat(summary.remainingLine()).isNull();
        assertThat(summary.amountToNextEpisodeLine()).isEqualTo("До следующей серии осталось: 4 500,00 ₽.");
    }

    @Test
    void from_emptyMonthHasNoPaymentLines() {
        ProjectMonthlyReport report = sampleReport(
                BigDecimal.ZERO.setScale(2),
                EpisodeFunding.calculate(BigDecimal.ZERO.setScale(2), new BigDecimal("4500.00")));

        ProjectMonthlySummaryView summary = ProjectMonthlySummaryView.from(
                new ProjectMonthlyReport(
                        report.month(),
                        report.monthLabel(),
                        false,
                        MonthlyPaymentTotals.empty(),
                        report.episodeFunding(),
                        report.totalGrossRubFormatted(),
                        report.totalNetRubFormatted(),
                        report.episodeCostRubFormatted(),
                        report.remainingRubFormatted(),
                        report.amountToNextEpisodeFormatted(),
                        report.sourceBreakdown(),
                        report.participantContributions()));

        assertThat(summary.hasPayments()).isFalse();
        assertThat(summary.totalNetRubFormatted()).isNull();
    }

    private ProjectMonthlyReport sampleReport(BigDecimal totalNet, EpisodeFunding episodeFunding) {
        ReportFormatter formatter = new ReportFormatter();
        MonthlyPaymentTotals totals = new MonthlyPaymentTotals(1, totalNet, totalNet);
        return new ProjectMonthlyReport(
                java.time.YearMonth.of(2026, 6),
                "Июнь 2026",
                true,
                totals,
                episodeFunding,
                formatter.formatRub(totalNet),
                formatter.formatRub(totalNet),
                formatter.formatRub(new BigDecimal("4500.00")),
                formatter.formatRub(episodeFunding.remainingRub()),
                formatter.formatRub(episodeFunding.amountToNextEpisode()),
                java.util.List.of(),
                java.util.List.of());
    }
}
