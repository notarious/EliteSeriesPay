package com.eliteseriespay.report;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record ProjectMonthlyReport(YearMonth month,
                                   String monthLabel,
                                   boolean hasPayments,
                                   MonthlyPaymentTotals totals,
                                   EpisodeFunding episodeFunding,
                                   String totalGrossRubFormatted,
                                   String totalNetRubFormatted,
                                   String episodeCostRubFormatted,
                                   String remainingRubFormatted,
                                   String amountToNextEpisodeFormatted,
                                   List<SourceBreakdownRow> sourceBreakdown,
                                   List<ParticipantContributionRow> participantContributions) {
}
