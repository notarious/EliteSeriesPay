package com.eliteseriespay.validation;

import com.eliteseriespay.exception.ValidationException;
import java.math.BigDecimal;

public final class ProjectValidator {

    private ProjectValidator() {
    }

    public static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException(ValidationError.PROJECT_NAME_REQUIRED);
        }
    }

    public static void validateEpisodeCost(BigDecimal episodeCostRub) {
        if (episodeCostRub == null) {
            throw new ValidationException(ValidationError.EPISODE_COST_REQUIRED);
        }
        if (episodeCostRub.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(ValidationError.EPISODE_COST_NOT_POSITIVE);
        }
    }

    public static void validateMonthlyFeeRub(BigDecimal monthlyFeeRub) {
        if (monthlyFeeRub == null) {
            throw new ValidationException(ValidationError.MONTHLY_FEE_RUB_REQUIRED);
        }
        if (monthlyFeeRub.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(ValidationError.MONTHLY_FEE_RUB_NOT_POSITIVE);
        }
    }

    public static void validateMonthlyFeeEur(BigDecimal monthlyFeeEur) {
        if (monthlyFeeEur == null) {
            throw new ValidationException(ValidationError.MONTHLY_FEE_EUR_REQUIRED);
        }
        if (monthlyFeeEur.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(ValidationError.MONTHLY_FEE_EUR_NOT_POSITIVE);
        }
    }
}
