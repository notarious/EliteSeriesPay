package com.eliteseriespay.report;

import com.eliteseriespay.domain.PaymentSource;
import com.eliteseriespay.payment.history.PaymentHistoryFormatter;
import com.eliteseriespay.repository.PaymentRepository;
import com.eliteseriespay.service.ProjectService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectReportService {

    private final PaymentRepository paymentRepository;
    private final ProjectService projectService;
    private final ReportFormatter reportFormatter;
    private final PaymentHistoryFormatter paymentHistoryFormatter;

    public ProjectReportService(PaymentRepository paymentRepository,
                                ProjectService projectService,
                                ReportFormatter reportFormatter,
                                PaymentHistoryFormatter paymentHistoryFormatter) {
        this.paymentRepository = paymentRepository;
        this.projectService = projectService;
        this.reportFormatter = reportFormatter;
        this.paymentHistoryFormatter = paymentHistoryFormatter;
    }

    @Transactional(readOnly = true)
    public ProjectMonthlyReport buildMonthlyReport(Long projectId, YearMonth month) {
        var project = projectService.findById(projectId);
        LocalDate dateFrom = month.atDay(1);
        LocalDate dateTo = month.atEndOfMonth();

        MonthlyPaymentTotals totals = loadTotals(projectId, dateFrom, dateTo);
        EpisodeFunding episodeFunding = EpisodeFunding.calculate(
                totals.totalNetRub(), project.getEpisodeCostRub());

        List<SourceBreakdownRow> sourceBreakdown = loadSourceBreakdown(projectId, dateFrom, dateTo);
        List<ParticipantContributionRow> participantContributions =
                loadParticipantContributions(projectId, dateFrom, dateTo);

        return new ProjectMonthlyReport(
                month,
                reportFormatter.formatMonthLabel(month),
                totals.hasPayments(),
                totals,
                episodeFunding,
                reportFormatter.formatRub(totals.totalGrossRub()),
                reportFormatter.formatRub(totals.totalNetRub()),
                reportFormatter.formatRub(project.getEpisodeCostRub()),
                reportFormatter.formatRub(episodeFunding.remainingRub()),
                reportFormatter.formatRub(episodeFunding.amountToNextEpisode()),
                sourceBreakdown,
                participantContributions);
    }

    @Transactional(readOnly = true)
    public ProjectMonthlySummaryView buildCurrentMonthSummary(Long projectId) {
        YearMonth currentMonth = YearMonth.now();
        ProjectMonthlyReport report = buildMonthlyReport(projectId, currentMonth);
        String summaryTitle = reportFormatter.formatBillingCollectionSummaryTitle(currentMonth);
        return ProjectMonthlySummaryView.from(report, summaryTitle);
    }

    private MonthlyPaymentTotals loadTotals(Long projectId, LocalDate dateFrom, LocalDate dateTo) {
        MonthlyPaymentAggregate aggregate = paymentRepository.sumActivePaymentsByProjectAndDateRange(
                projectId, dateFrom, dateTo);
        if (aggregate == null) {
            return MonthlyPaymentTotals.empty();
        }
        return aggregate.toTotals();
    }

    private List<SourceBreakdownRow> loadSourceBreakdown(Long projectId,
                                                       LocalDate dateFrom,
                                                       LocalDate dateTo) {
        List<SourcePaymentAggregate> rows = paymentRepository
                .sumActivePaymentsByProjectAndDateRangeGroupBySource(projectId, dateFrom, dateTo);

        Map<PaymentSource, SourceBreakdownRow> bySource = new EnumMap<>(PaymentSource.class);
        for (SourcePaymentAggregate row : rows) {
            bySource.put(row.source(), new SourceBreakdownRow(
                    paymentHistoryFormatter.sourceLabel(row.source()),
                    row.source(),
                    row.paymentCount(),
                    reportFormatter.formatRub(row.totalGrossRub()),
                    reportFormatter.formatRub(row.totalNetRub())));
        }

        List<SourceBreakdownRow> result = new ArrayList<>();
        for (PaymentSource source : PaymentSource.values()) {
            SourceBreakdownRow existing = bySource.get(source);
            if (existing != null) {
                result.add(existing);
            } else {
                result.add(new SourceBreakdownRow(
                        paymentHistoryFormatter.sourceLabel(source),
                        source,
                        0,
                        reportFormatter.formatRub(java.math.BigDecimal.ZERO),
                        reportFormatter.formatRub(java.math.BigDecimal.ZERO)));
            }
        }
        return result;
    }

    private List<ParticipantContributionRow> loadParticipantContributions(Long projectId,
                                                                          LocalDate dateFrom,
                                                                          LocalDate dateTo) {
        return paymentRepository.sumNetByParticipantForProjectAndDateRange(projectId, dateFrom, dateTo).stream()
                .map(row -> new ParticipantContributionRow(
                        row.participantName(),
                        paymentHistoryFormatter.billingModeLabel(row.billingMode()),
                        reportFormatter.formatRub(row.totalNetRub())))
                .toList();
    }
}
