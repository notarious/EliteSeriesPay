package com.eliteseriespay.validation;

import com.eliteseriespay.domain.PaymentCurrency;
import com.eliteseriespay.domain.PaymentSource;
import com.eliteseriespay.exception.ValidationException;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class PaymentValidator {

    private PaymentValidator() {
    }

    public static void validate(LocalDate paymentDate,
                                PaymentSource source,
                                BigDecimal amountOriginal,
                                PaymentCurrency currency,
                                BigDecimal exchangeRate) {
        if (paymentDate == null) {
            throw new ValidationException(ValidationError.PAYMENT_DATE_REQUIRED);
        }
        if (source == null) {
            throw new ValidationException(ValidationError.PAYMENT_SOURCE_REQUIRED);
        }
        if (amountOriginal == null) {
            throw new ValidationException(ValidationError.PAYMENT_AMOUNT_REQUIRED);
        }
        if (amountOriginal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(ValidationError.PAYMENT_AMOUNT_NOT_POSITIVE);
        }
        if (currency == null) {
            throw new ValidationException(ValidationError.PAYMENT_CURRENCY_REQUIRED);
        }
        if (currency != PaymentCurrency.RUB) {
            if (exchangeRate == null) {
                throw new ValidationException(ValidationError.EXCHANGE_RATE_REQUIRED);
            }
            if (exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException(ValidationError.EXCHANGE_RATE_NOT_POSITIVE);
            }
        }
    }
}
