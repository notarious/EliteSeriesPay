package com.eliteseriespay.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eliteseriespay.domain.PaymentCurrency;
import com.eliteseriespay.domain.Project;
import com.eliteseriespay.exception.ValidationException;
import com.eliteseriespay.format.AmountFormatter;
import com.eliteseriespay.support.TestEntities;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class InitialSubscriptionPaymentValidatorTest {

    @Test
    void validate_rejectsRubAmountBelowMonthlyFee() {
        Project project = projectWithFees(new BigDecimal("600.00"), new BigDecimal("8.00"));

        assertThatThrownBy(() -> InitialSubscriptionPaymentValidator.validate(
                project, new BigDecimal("599.99"), PaymentCurrency.RUB))
                .isInstanceOf(ValidationException.class)
                .hasMessage(requiredAmountMessage(new BigDecimal("600.00"), PaymentCurrency.RUB));
    }

    @Test
    void validate_messageUsesProjectMonthlyFeeRub() {
        Project project = projectWithFees(new BigDecimal("600.00"), new BigDecimal("8.00"));

        assertThatThrownBy(() -> InitialSubscriptionPaymentValidator.validate(
                project, new BigDecimal("100.00"), PaymentCurrency.RUB))
                .isInstanceOf(ValidationException.class)
                .hasMessage(requiredAmountMessage(project.getMonthlyFeeRub(), PaymentCurrency.RUB));
    }

    @Test
    void validate_messageUsesProjectMonthlyFeeEur() {
        Project project = projectWithFees(new BigDecimal("600.00"), new BigDecimal("8.00"));

        assertThatThrownBy(() -> InitialSubscriptionPaymentValidator.validate(
                project, new BigDecimal("1.00"), PaymentCurrency.EUR))
                .isInstanceOf(ValidationException.class)
                .hasMessage(requiredAmountMessage(project.getMonthlyFeeEur(), PaymentCurrency.EUR));
    }

    @Test
    void validate_messageReflectsDifferentProjectSettings() {
        Project cheaperProject = projectWithFees(new BigDecimal("300.00"), new BigDecimal("4.00"));
        Project expensiveProject = projectWithFees(new BigDecimal("900.00"), new BigDecimal("12.00"));

        assertThatThrownBy(() -> InitialSubscriptionPaymentValidator.validate(
                cheaperProject, new BigDecimal("100.00"), PaymentCurrency.RUB))
                .hasMessage(requiredAmountMessage(cheaperProject.getMonthlyFeeRub(), PaymentCurrency.RUB));

        assertThatThrownBy(() -> InitialSubscriptionPaymentValidator.validate(
                expensiveProject, new BigDecimal("100.00"), PaymentCurrency.RUB))
                .hasMessage(requiredAmountMessage(expensiveProject.getMonthlyFeeRub(), PaymentCurrency.RUB));
    }

    @Test
    void validate_messageShowsRequiredAmountForThreeHundredRubProject() {
        Project project = projectWithFees(new BigDecimal("300.00"), new BigDecimal("4.00"));

        assertThatThrownBy(() -> InitialSubscriptionPaymentValidator.validate(
                project, new BigDecimal("100.00"), PaymentCurrency.RUB))
                .hasMessage(
                        "Для вступления в проект необходимо оплатить полный месячный абонемент: 300,00 ₽");
    }

    @Test
    void validate_acceptsRubAmountEqualToMonthlyFee() {
        Project project = projectWithFees(new BigDecimal("600.00"), new BigDecimal("8.00"));

        assertThatCode(() -> InitialSubscriptionPaymentValidator.validate(
                project, project.getMonthlyFeeRub(), PaymentCurrency.RUB))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_acceptsRubAmountAboveMonthlyFee() {
        Project project = projectWithFees(new BigDecimal("600.00"), new BigDecimal("8.00"));

        assertThatCode(() -> InitialSubscriptionPaymentValidator.validate(
                project, new BigDecimal("750.00"), PaymentCurrency.RUB))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_rejectsEurAmountBelowMonthlyFee() {
        Project project = projectWithFees(new BigDecimal("600.00"), new BigDecimal("8.00"));

        assertThatThrownBy(() -> InitialSubscriptionPaymentValidator.validate(
                project, new BigDecimal("7.99"), PaymentCurrency.EUR))
                .isInstanceOf(ValidationException.class)
                .hasMessage(requiredAmountMessage(project.getMonthlyFeeEur(), PaymentCurrency.EUR));
    }

    @Test
    void validate_acceptsEurAmountEqualToMonthlyFee() {
        Project project = projectWithFees(new BigDecimal("600.00"), new BigDecimal("8.00"));

        assertThatCode(() -> InitialSubscriptionPaymentValidator.validate(
                project, project.getMonthlyFeeEur(), PaymentCurrency.EUR))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_rejectsUsdCurrency() {
        Project project = projectWithFees(new BigDecimal("600.00"), new BigDecimal("8.00"));

        assertThatThrownBy(() -> InitialSubscriptionPaymentValidator.validate(
                project, new BigDecimal("10.00"), PaymentCurrency.USD))
                .isInstanceOf(ValidationException.class)
                .hasMessage(ValidationError.INITIAL_SUBSCRIPTION_PAYMENT_USD_NOT_SUPPORTED.getMessage());
    }

    private static Project projectWithFees(BigDecimal monthlyFeeRub, BigDecimal monthlyFeeEur) {
        return TestEntities.project(1L, "Series", new BigDecimal("1000.00"), monthlyFeeRub, monthlyFeeEur);
    }

    private static String requiredAmountMessage(BigDecimal amount, PaymentCurrency currency) {
        return ValidationError.INITIAL_SUBSCRIPTION_PAYMENT_INSUFFICIENT.getMessage()
                + ": "
                + AmountFormatter.formatWithCurrency(amount, currency);
    }
}
