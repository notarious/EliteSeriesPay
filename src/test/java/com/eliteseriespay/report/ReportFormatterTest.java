package com.eliteseriespay.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class ReportFormatterTest {

    private final ReportFormatter reportFormatter = new ReportFormatter();

    @Test
    void formatBillingCollectionSummaryTitle_usesNextMonth() {
        assertThat(reportFormatter.formatBillingCollectionSummaryTitle(YearMonth.of(2026, 6)))
                .isEqualTo("Сборы на июль 2026");
    }

    @Test
    void formatBillingCollectionSummaryTitle_handlesYearTransition() {
        assertThat(reportFormatter.formatBillingCollectionSummaryTitle(YearMonth.of(2026, 12)))
                .isEqualTo("Сборы на январь 2027");
    }

    @Test
    void formatBillingCollectionSummaryTitle_usesLowercaseMonthName() {
        assertThat(reportFormatter.formatBillingCollectionSummaryTitle(YearMonth.of(2026, 1)))
                .isEqualTo("Сборы на февраль 2026");
    }

    @Test
    void formatMonthLabel_keepsCapitalizedMonthForReports() {
        assertThat(reportFormatter.formatMonthLabel(YearMonth.of(2026, 6)))
                .isEqualTo("Июнь 2026");
    }
}
