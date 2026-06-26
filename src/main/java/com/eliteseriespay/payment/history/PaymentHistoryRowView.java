package com.eliteseriespay.payment.history;

public record PaymentHistoryRowView(long paymentId,
                                    long participantId,
                                    String paymentDate,
                                    String participantName,
                                    String billingModeLabel,
                                    String sourceLabel,
                                    String amountOriginal,
                                    String currencyLabel,
                                    String exchangeRate,
                                    String amountRub,
                                    String netAmountRub,
                                    String statusLabel,
                                    boolean voided,
                                    String comment,
                                    boolean editable) {
}
