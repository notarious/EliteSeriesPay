package com.eliteseriespay.service;

import com.eliteseriespay.domain.ProjectMembership;

public record MembershipAddResult(MembershipAddAction action,
                                  Long participantId,
                                  ProjectMembership membership) {

    public static MembershipAddResult added(ProjectMembership membership) {
        return new MembershipAddResult(
                MembershipAddAction.ADDED, membership.getParticipant().getId(), membership);
    }

    public static MembershipAddResult requiresInitialPayment(Long participantId) {
        return new MembershipAddResult(MembershipAddAction.REQUIRES_INITIAL_PAYMENT, participantId, null);
    }

    public boolean requiresInitialPayment() {
        return action == MembershipAddAction.REQUIRES_INITIAL_PAYMENT;
    }
}
