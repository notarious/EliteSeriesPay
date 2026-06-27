package com.eliteseriespay.billing;

public record CurrentMonthPaymentInfo(String label, String cssClass) {

    public static CurrentMonthPaymentInfo paid() {
        return new CurrentMonthPaymentInfo("✅ Оплачено", "text-success");
    }

    public static CurrentMonthPaymentInfo notPaid() {
        return new CurrentMonthPaymentInfo("🕒 Не оплачено", "text-warning");
    }

    public static CurrentMonthPaymentInfo debt(String formattedAmount) {
        return new CurrentMonthPaymentInfo("❌ Долг " + formattedAmount, "text-danger");
    }

    public static CurrentMonthPaymentInfo notApplicable() {
        return new CurrentMonthPaymentInfo("—", "text-muted");
    }
}
