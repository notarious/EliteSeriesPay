package com.eliteseriespay.report;

import java.math.BigDecimal;

public record ParticipantContributionRow(String participantName,
                                         String billingModeLabel,
                                         String netContributionRub) {
}
