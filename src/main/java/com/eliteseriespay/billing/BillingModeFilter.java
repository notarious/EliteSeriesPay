package com.eliteseriespay.billing;

import com.eliteseriespay.domain.BillingMode;

public record BillingModeFilter(BillingMode mode) {

    public static final BillingModeFilter ALL = new BillingModeFilter(null);

    public static BillingModeFilter of(BillingMode mode) {
        if (mode == null) {
            return ALL;
        }
        return new BillingModeFilter(mode);
    }

    public boolean isAll() {
        return mode == null;
    }
}
