package com.eliteseriespay.validation;

import com.eliteseriespay.domain.PaymentCurrency;
import com.eliteseriespay.domain.Project;
import com.eliteseriespay.exception.ValidationException;
import java.math.BigDecimal;

public final class InitialSubscriptionPaymentValidator {

    private InitialSubscriptionPaymentValidator() {
    }

    public static void validate(Project project, BigDecimal amountOriginal, PaymentCurrency currency) {
        if (currency == PaymentCurrency.USD) {
            throw new ValidationException(ValidationError.INITIAL_SUBSCRIPTION_PAYMENT_USD_NOT_SUPPORTED);
        }

        BigDecimal requiredAmount = switch (currency) {
            case RUB -> project.getMonthlyFeeRub();
            case EUR -> project.getMonthlyFeeEur();
            case USD -> throw new ValidationException(ValidationError.INITIAL_SUBSCRIPTION_PAYMENT_USD_NOT_SUPPORTED);
        };

        if (amountOriginal.compareTo(requiredAmount) < 0) {
            String message = ValidationError.INITIAL_SUBSCRIPTION_PAYMENT_INSUFFICIENT.getMessage()
                    + ": " + formatRequiredAmount(requiredAmount, currency) + ".";
            throw new ValidationException(ValidationError.INITIAL_SUBSCRIPTION_PAYMENT_INSUFFICIENT, message);
        }
    }

    private static String formatRequiredAmount(BigDecimal amount, PaymentCurrency currency) {
        return amount.stripTrailingZeros().toPlainString() + " " + currency.getDisplayName();
    }
}
