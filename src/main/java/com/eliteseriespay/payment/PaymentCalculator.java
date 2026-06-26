package com.eliteseriespay.payment;

import com.eliteseriespay.domain.PaymentCurrency;
import com.eliteseriespay.domain.PaymentSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class PaymentCalculator {

    private static final int MONEY_SCALE = 2;
    private static final int EXCHANGE_RATE_SCALE = 4;

    public NormalizedAmounts normalize(BigDecimal amountOriginal,
                                         PaymentCurrency currency,
                                         BigDecimal exchangeRate) {
        return new NormalizedAmounts(
                normalizeAmount(amountOriginal),
                normalizeExchangeRate(currency, exchangeRate));
    }

    public PaymentAmounts calculate(PaymentSource source, NormalizedAmounts normalized, int vkDonutFeePercent) {
        BigDecimal amountRub = calculateAmountRub(normalized.amountOriginal(), normalized.exchangeRate());
        int feePercent = calculateFeePercent(source, vkDonutFeePercent);
        BigDecimal netAmountRub = calculateNetAmountRub(amountRub, feePercent);

        return new PaymentAmounts(
                normalized.amountOriginal(),
                normalized.exchangeRate(),
                amountRub,
                feePercent,
                netAmountRub);
    }

    private BigDecimal normalizeAmount(BigDecimal amountOriginal) {
        if (amountOriginal == null) {
            return null;
        }
        return amountOriginal.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeExchangeRate(PaymentCurrency currency, BigDecimal exchangeRate) {
        if (currency == PaymentCurrency.RUB) {
            return BigDecimal.ONE.setScale(EXCHANGE_RATE_SCALE, RoundingMode.HALF_UP);
        }
        if (exchangeRate == null) {
            return null;
        }
        return exchangeRate.setScale(EXCHANGE_RATE_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAmountRub(BigDecimal amountOriginal, BigDecimal exchangeRate) {
        return amountOriginal.multiply(exchangeRate).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private int calculateFeePercent(PaymentSource source, int vkDonutFeePercent) {
        return switch (source) {
            case VK_DONUT -> vkDonutFeePercent;
            case MANUAL -> 0;
        };
    }

    private BigDecimal calculateNetAmountRub(BigDecimal amountRub, int feePercent) {
        return amountRub
                .multiply(BigDecimal.valueOf(100 - feePercent))
                .divide(BigDecimal.valueOf(100), MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
