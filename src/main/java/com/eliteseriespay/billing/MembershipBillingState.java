package com.eliteseriespay.billing;

import com.eliteseriespay.domain.PaymentCurrency;
import java.math.BigDecimal;
import java.time.YearMonth;

public class MembershipBillingState {

    private YearMonth paidUntilMonth;
    private BigDecimal partialPaymentAmount;
    private PaymentCurrency partialPaymentCurrency;

    public static MembershipBillingState empty() {
        return new MembershipBillingState();
    }

    public YearMonth paidUntilMonth() {
        return paidUntilMonth;
    }

    public void setPaidUntilMonth(YearMonth paidUntilMonth) {
        this.paidUntilMonth = paidUntilMonth;
    }

    public BigDecimal partialPaymentAmount() {
        return partialPaymentAmount;
    }

    public PaymentCurrency partialPaymentCurrency() {
        return partialPaymentCurrency;
    }

    public boolean hasPartial() {
        return partialPaymentAmount != null
                && partialPaymentAmount.compareTo(BigDecimal.ZERO) > 0
                && partialPaymentCurrency != null;
    }

    public void setPartialPayment(BigDecimal amount, PaymentCurrency currency) {
        this.partialPaymentAmount = amount;
        this.partialPaymentCurrency = currency;
    }

    public void clearPartialPayment() {
        this.partialPaymentAmount = null;
        this.partialPaymentCurrency = null;
    }
}
