package com.eliteseriespay.report;

public record ProjectMonthlySummaryView(String monthLabel,
                                        boolean hasPayments,
                                        String totalNetRubFormatted,
                                        String episodeCostRubFormatted,
                                        String episodesFundedLine,
                                        String remainingLine,
                                        String amountToNextEpisodeLine) {

    public static ProjectMonthlySummaryView from(ProjectMonthlyReport report) {
        return from(report, report.monthLabel());
    }

    public static ProjectMonthlySummaryView from(ProjectMonthlyReport report, String monthLabel) {
        if (!report.hasPayments()) {
            return new ProjectMonthlySummaryView(
                    monthLabel,
                    false,
                    null,
                    null,
                    null,
                    null,
                    null);
        }

        String episodesLine = ReportText.episodesFundedLine(report.episodeFunding().episodesFunded());
        String remainingLine = null;
        String amountToNextLine;

        if (report.episodeFunding().remainingRub().signum() > 0) {
            remainingLine = "Остаток: " + report.remainingRubFormatted() + ".";
            int nextEpisode = report.episodeFunding().episodesFunded() + 1;
            amountToNextLine = "До " + ReportText.ordinalEpisode(nextEpisode)
                    + " серии осталось: " + report.amountToNextEpisodeFormatted() + ".";
        } else {
            amountToNextLine = "До следующей серии осталось: " + report.episodeCostRubFormatted() + ".";
        }

        return new ProjectMonthlySummaryView(
                monthLabel,
                true,
                report.totalNetRubFormatted(),
                report.episodeCostRubFormatted(),
                episodesLine,
                remainingLine,
                amountToNextLine);
    }
}
