package com.eliteseriespay.report;

import com.eliteseriespay.domain.BillingMode;
import java.math.BigDecimal;

public record ParticipantNetAggregate(String participantName,
                                      BillingMode billingMode,
                                      BigDecimal totalNetRub) {
}
