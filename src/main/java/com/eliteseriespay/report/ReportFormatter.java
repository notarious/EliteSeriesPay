package com.eliteseriespay.report;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ReportFormatter {

    private static final DecimalFormat AMOUNT_FORMAT = amountFormat(2);
    private static final DateTimeFormatter MONTH_LABEL_FORMAT =
            DateTimeFormatter.ofPattern("LLLL yyyy", Locale.forLanguageTag("ru"));

    public String formatMonthLabel(YearMonth month) {
        String label = month.atDay(1).format(MONTH_LABEL_FORMAT);
        if (label.isEmpty()) {
            return label;
        }
        return Character.toUpperCase(label.charAt(0)) + label.substring(1);
    }

    public String formatCurrentMonthPaymentColumnTitle(YearMonth month) {
        String monthName = month.atDay(1).format(
                DateTimeFormatter.ofPattern("LLLL", Locale.forLanguageTag("ru")));
        return "Оплата за " + monthName;
    }

    public String formatRub(BigDecimal amount) {
        if (amount == null) {
            return "—";
        }
        return formatDecimal(amount) + " ₽";
    }

    public String formatEur(BigDecimal amount) {
        if (amount == null) {
            return "—";
        }
        return formatDecimal(amount) + " €";
    }

    public String formatCount(long count) {
        return String.valueOf(count);
    }

    private String formatDecimal(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("ru-RU"));
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator(' ');
        DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
        format.setGroupingUsed(true);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format(value);
    }

    private static DecimalFormat amountFormat(int fractionDigits) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("ru-RU"));
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator(' ');
        DecimalFormat format = new DecimalFormat("#,##0." + "0".repeat(fractionDigits), symbols);
        format.setGroupingUsed(true);
        format.setMinimumFractionDigits(fractionDigits);
        format.setMaximumFractionDigits(fractionDigits);
        return format;
    }
}
