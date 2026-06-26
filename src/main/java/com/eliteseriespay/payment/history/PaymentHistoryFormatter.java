package com.eliteseriespay.payment.history;

import com.eliteseriespay.domain.BillingMode;
import com.eliteseriespay.domain.Payment;
import com.eliteseriespay.domain.PaymentCurrency;
import com.eliteseriespay.domain.PaymentSource;
import com.eliteseriespay.domain.PaymentStatus;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class PaymentHistoryFormatter {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DecimalFormat AMOUNT_FORMAT = amountFormat(2);
    private static final DecimalFormat RATE_FORMAT = amountFormat(4);

    public PaymentHistoryRowView toRowView(Payment payment, BillingMode billingMode) {
        return new PaymentHistoryRowView(
                payment.getId(),
                payment.getParticipant().getId(),
                DATE_FORMAT.format(payment.getPaymentDate()),
                payment.getParticipant().getName(),
                billingModeLabel(billingMode),
                sourceLabel(payment.getSource()),
                formatAmount(payment.getAmountOriginal()),
                currencyLabel(payment.getCurrency()),
                formatExchangeRate(payment.getCurrency(), payment.getExchangeRate()),
                formatAmount(payment.getAmountRub()) + " ₽",
                formatAmount(payment.getNetAmountRub()) + " ₽",
                statusLabel(payment.getStatus()),
                payment.getStatus() == PaymentStatus.VOIDED,
                payment.getComment() != null && !payment.getComment().isBlank()
                        ? payment.getComment()
                        : null,
                payment.getStatus() == PaymentStatus.ACTIVE);
    }

    public String sourceLabel(PaymentSource source) {
        return source.getDisplayName();
    }

    public String currencyLabel(PaymentCurrency currency) {
        return currency.name();
    }

    public String billingModeLabel(BillingMode billingMode) {
        if (billingMode == null) {
            return "—";
        }
        return billingMode.getDisplayName();
    }

    public String statusLabel(PaymentStatus status) {
        return status.getDisplayName();
    }

    private String formatExchangeRate(PaymentCurrency currency, BigDecimal exchangeRate) {
        if (currency == PaymentCurrency.RUB) {
            return "—";
        }
        return formatDecimal(exchangeRate, RATE_FORMAT);
    }

    private String formatAmount(BigDecimal amount) {
        return formatDecimal(amount, AMOUNT_FORMAT);
    }

    private static String formatDecimal(BigDecimal value, DecimalFormat format) {
        if (value == null) {
            return "—";
        }
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
