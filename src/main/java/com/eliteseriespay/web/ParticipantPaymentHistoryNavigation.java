package com.eliteseriespay.web;

import com.eliteseriespay.service.ParticipantPaymentHistoryFilter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.util.UriComponentsBuilder;

public final class ParticipantPaymentHistoryNavigation {

    private final long participantId;
    private final ParticipantPaymentHistoryFilter filter;

    private ParticipantPaymentHistoryNavigation(long participantId, ParticipantPaymentHistoryFilter filter) {
        this.participantId = participantId;
        this.filter = filter;
    }

    public static ParticipantPaymentHistoryNavigation of(long participantId,
                                                         ParticipantPaymentHistoryFilter filter) {
        return new ParticipantPaymentHistoryNavigation(participantId, filter);
    }

    public String listUrl() {
        return listUrl(filter.page());
    }

    public String listUrl(int page) {
        ParticipantPaymentHistoryFilter pageFilter = ParticipantPaymentHistoryFilter.of(
                filter.projectId(),
                filter.source(),
                filter.status(),
                filter.dateFrom(),
                filter.dateTo(),
                page,
                filter.pageSize());
        UriComponentsBuilder builder = listPathBuilder();
        appendListQuery(builder, pageFilter);
        return builder.build().toUriString();
    }

    public String editUrl(long paymentId) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromPath("/participants/" + participantId + "/payments/" + paymentId + "/edit");
        appendEditRoundTripQuery(builder, filter);
        return builder.build().toUriString();
    }

    public List<PaymentHistoryFormParam> formParams() {
        List<PaymentHistoryFormParam> params = new ArrayList<>();
        if (filter.projectId() != null) {
            params.add(new PaymentHistoryFormParam("historyProjectId", filter.projectId()));
        }
        if (filter.source() != null) {
            params.add(new PaymentHistoryFormParam("historySource", filter.source()));
        }
        if (filter.status() != null) {
            params.add(new PaymentHistoryFormParam("historyStatus", filter.status()));
        }
        if (filter.dateFrom() != null) {
            params.add(new PaymentHistoryFormParam("historyDateFrom", filter.dateFrom()));
        }
        if (filter.dateTo() != null) {
            params.add(new PaymentHistoryFormParam("historyDateTo", filter.dateTo()));
        }
        params.add(new PaymentHistoryFormParam("historyPage", filter.page()));
        params.add(new PaymentHistoryFormParam("historySize", filter.pageSize()));
        return List.copyOf(params);
    }

    public static void appendListQuery(UriComponentsBuilder builder, ParticipantPaymentHistoryFilter filter) {
        if (filter.projectId() != null) {
            builder.queryParam("projectId", filter.projectId());
        }
        if (filter.source() != null) {
            builder.queryParam("source", filter.source());
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
        if (filter.pageSize() != ParticipantPaymentHistoryFilter.DEFAULT_PAGE_SIZE) {
            builder.queryParam("size", filter.pageSize());
        }
    }

    public static void appendEditRoundTripQuery(UriComponentsBuilder builder,
                                                ParticipantPaymentHistoryFilter filter) {
        if (filter.projectId() != null) {
            builder.queryParam("historyProjectId", filter.projectId());
        }
        if (filter.source() != null) {
            builder.queryParam("historySource", filter.source());
        }
        if (filter.status() != null) {
            builder.queryParam("historyStatus", filter.status());
        }
        if (filter.dateFrom() != null) {
            builder.queryParam("historyDateFrom", filter.dateFrom());
        }
        if (filter.dateTo() != null) {
            builder.queryParam("historyDateTo", filter.dateTo());
        }
        builder.queryParam("historyPage", filter.page());
        builder.queryParam("historySize", filter.pageSize());
    }

    private UriComponentsBuilder listPathBuilder() {
        return UriComponentsBuilder.fromPath("/participants/" + participantId + "/payments");
    }
}
