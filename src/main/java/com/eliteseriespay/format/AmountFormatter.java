package com.eliteseriespay.format;

import com.eliteseriespay.domain.PaymentCurrency;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class AmountFormatter {

    private AmountFormatter() {
    }

    public static String formatWithCurrency(BigDecimal amount, PaymentCurrency currency) {
        String formatted = formatDecimal(amount);
        return switch (currency) {
            case RUB -> formatted + " ₽";
            case EUR -> formatted + " €";
            case USD -> "$" + formatted;
        };
    }

    private static String formatDecimal(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("ru-RU"));
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator(' ');
        DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
        format.setGroupingUsed(true);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format(value);
    }
}
