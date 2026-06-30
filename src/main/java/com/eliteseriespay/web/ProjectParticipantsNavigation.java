package com.eliteseriespay.web;

import com.eliteseriespay.billing.MembershipPaymentStatusFilter;
import com.eliteseriespay.domain.BillingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.util.UriComponentsBuilder;

public final class ProjectParticipantsNavigation {

    private final long projectId;
    private final ProjectParticipantsFilter filter;

    private ProjectParticipantsNavigation(long projectId, ProjectParticipantsFilter filter) {
        this.projectId = projectId;
        this.filter = filter;
    }

    public static ProjectParticipantsNavigation of(long projectId, ProjectParticipantsFilter filter) {
        return new ProjectParticipantsNavigation(projectId, filter);
    }

    public String listUrl() {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/projects/" + projectId);
        appendListQuery(builder, filter);
        return builder.build().toUriString();
    }

    public List<PaymentHistoryFormParam> formParams() {
        List<PaymentHistoryFormParam> params = new ArrayList<>();
        if (filter.billingMode() != null) {
            params.add(new PaymentHistoryFormParam("billingMode", filter.billingMode()));
        }
        if (filter.paymentStatus() != null && filter.paymentStatus() != MembershipPaymentStatusFilter.ALL) {
            params.add(new PaymentHistoryFormParam("paymentStatus", filter.paymentStatus()));
        }
        return List.copyOf(params);
    }

    public static String listUrl(long projectId, ProjectParticipantsFilter filter) {
        return of(projectId, filter).listUrl();
    }

    public static void appendListQuery(UriComponentsBuilder builder, ProjectParticipantsFilter filter) {
        if (filter.billingMode() != null) {
            builder.queryParam("billingMode", filter.billingMode());
        }
        if (filter.paymentStatus() != null && filter.paymentStatus() != MembershipPaymentStatusFilter.ALL) {
            builder.queryParam("paymentStatus", filter.paymentStatus());
        }
    }

    public static ProjectParticipantsFilter filterFromParams(BillingMode billingMode,
                                                             MembershipPaymentStatusFilter paymentStatus) {
        MembershipPaymentStatusFilter resolvedStatus = paymentStatus != null && paymentStatus != MembershipPaymentStatusFilter.ALL
                ? paymentStatus
                : null;
        return ProjectParticipantsFilter.of(billingMode, resolvedStatus);
    }
}
