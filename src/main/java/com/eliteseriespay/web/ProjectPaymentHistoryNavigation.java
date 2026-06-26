package com.eliteseriespay.web;

import com.eliteseriespay.payment.history.ProjectPaymentHistoryFilter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.util.UriComponentsBuilder;

public final class ProjectPaymentHistoryNavigation {

    private final long projectId;
    private final ProjectPaymentHistoryFilter filter;

    private ProjectPaymentHistoryNavigation(long projectId, ProjectPaymentHistoryFilter filter) {
        this.projectId = projectId;
        this.filter = filter;
    }

    public static ProjectPaymentHistoryNavigation of(long projectId, ProjectPaymentHistoryFilter filter) {
        return new ProjectPaymentHistoryNavigation(projectId, filter);
    }

    public String listUrl() {
        return listUrl(filter.page());
    }

    public String listUrl(int page) {
        ProjectPaymentHistoryFilter pageFilter = ProjectPaymentHistoryFilter.of(
                filter.projectId(),
                filter.participantId(),
                filter.billingMode(),
                filter.source(),
                filter.currency(),
                filter.status(),
                filter.dateFrom(),
                filter.dateTo(),
                page,
                filter.pageSize());
        UriComponentsBuilder builder = listPathBuilder();
        appendListQuery(builder, pageFilter);
        return builder.build().toUriString();
    }

    public String editUrl(long participantId, long paymentId) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromPath("/participants/" + participantId + "/payments/" + paymentId + "/edit");
        appendEditRoundTripQuery(builder, projectId, filter);
        return builder.build().toUriString();
    }

    public List<PaymentHistoryFormParam> formParams() {
        List<PaymentHistoryFormParam> params = new ArrayList<>();
        params.add(new PaymentHistoryFormParam("projectHistoryProjectId", projectId));
        if (filter.participantId() != null) {
            params.add(new PaymentHistoryFormParam("projectHistoryParticipantId", filter.participantId()));
        }
        if (filter.billingMode() != null) {
            params.add(new PaymentHistoryFormParam("projectHistoryBillingMode", filter.billingMode()));
        }
        if (filter.source() != null) {
            params.add(new PaymentHistoryFormParam("projectHistorySource", filter.source()));
        }
        if (filter.currency() != null) {
            params.add(new PaymentHistoryFormParam("projectHistoryCurrency", filter.currency()));
        }
        if (filter.status() != null) {
            params.add(new PaymentHistoryFormParam("projectHistoryStatus", filter.status()));
        }
        if (filter.dateFrom() != null) {
            params.add(new PaymentHistoryFormParam("projectHistoryDateFrom", filter.dateFrom()));
        }
        if (filter.dateTo() != null) {
            params.add(new PaymentHistoryFormParam("projectHistoryDateTo", filter.dateTo()));
        }
        params.add(new PaymentHistoryFormParam("projectHistoryPage", filter.page()));
        params.add(new PaymentHistoryFormParam("projectHistorySize", filter.pageSize()));
        return List.copyOf(params);
    }

    public static ProjectPaymentHistoryFilter filterFromEditParams(Long projectHistoryProjectId,
                                                                   Long projectHistoryParticipantId,
                                                                   com.eliteseriespay.domain.BillingMode projectHistoryBillingMode,
                                                                   com.eliteseriespay.domain.PaymentSource projectHistorySource,
                                                                   com.eliteseriespay.domain.PaymentCurrency projectHistoryCurrency,
                                                                   com.eliteseriespay.domain.PaymentStatus projectHistoryStatus,
                                                                   java.time.LocalDate projectHistoryDateFrom,
                                                                   java.time.LocalDate projectHistoryDateTo,
                                                                   int projectHistoryPage,
                                                                   int projectHistorySize) {
        return ProjectPaymentHistoryFilter.of(
                projectHistoryProjectId,
                projectHistoryParticipantId,
                projectHistoryBillingMode,
                projectHistorySource,
                projectHistoryCurrency,
                projectHistoryStatus,
                projectHistoryDateFrom,
                projectHistoryDateTo,
                projectHistoryPage,
                projectHistorySize);
    }

    public static void appendListQuery(UriComponentsBuilder builder, ProjectPaymentHistoryFilter filter) {
        if (filter.participantId() != null) {
            builder.queryParam("participantId", filter.participantId());
        }
        if (filter.billingMode() != null) {
            builder.queryParam("billingMode", filter.billingMode());
        }
        if (filter.source() != null) {
            builder.queryParam("source", filter.source());
        }
        if (filter.currency() != null) {
            builder.queryParam("currency", filter.currency());
        }
        if (filter.status() != null) {
            builder.queryParam("status", filter.status());
        }
        if (filter.dateFrom() != null) {
            builder.queryParam("dateFrom", filter.dateFrom());
        }
        if (filter.dateTo() != null) {
            builder.queryParam("dateTo", filter.dateTo());
        }
        if (filter.page() > 0) {
            builder.queryParam("page", filter.page());
        }
        if (filter.pageSize() != ProjectPaymentHistoryFilter.DEFAULT_PAGE_SIZE) {
            builder.queryParam("size", filter.pageSize());
        }
    }

    public static void appendEditRoundTripQuery(UriComponentsBuilder builder,
                                                long projectId,
                                                ProjectPaymentHistoryFilter filter) {
        builder.queryParam("projectHistoryProjectId", projectId);
        if (filter.participantId() != null) {
            builder.queryParam("projectHistoryParticipantId", filter.participantId());
        }
        if (filter.billingMode() != null) {
            builder.queryParam("projectHistoryBillingMode", filter.billingMode());
        }
        if (filter.source() != null) {
            builder.queryParam("projectHistorySource", filter.source());
        }
        if (filter.currency() != null) {
            builder.queryParam("projectHistoryCurrency", filter.currency());
        }
        if (filter.status() != null) {
            builder.queryParam("projectHistoryStatus", filter.status());
        }
        if (filter.dateFrom() != null) {
            builder.queryParam("projectHistoryDateFrom", filter.dateFrom());
        }
        if (filter.dateTo() != null) {
            builder.queryParam("projectHistoryDateTo", filter.dateTo());
        }
        builder.queryParam("projectHistoryPage", filter.page());
        builder.queryParam("projectHistorySize", filter.pageSize());
    }

    private UriComponentsBuilder listPathBuilder() {
        return UriComponentsBuilder.fromPath("/projects/" + projectId + "/payments");
    }
}
