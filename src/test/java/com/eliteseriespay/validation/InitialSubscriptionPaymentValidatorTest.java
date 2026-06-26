package com.eliteseriespay.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eliteseriespay.domain.PaymentCurrency;
import com.eliteseriespay.domain.Project;
import com.eliteseriespay.exception.ValidationException;
import com.eliteseriespay.support.TestEntities;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InitialSubscriptionPaymentValidatorTest {

    private Project project;

    @BeforeEach
    void setUp() {
        project = TestEntities.project(1L, "Series", new BigDecimal("1000.00"),
                new BigDecimal("500.00"), new BigDecimal("7.00"));
    }

    @Test
    void validate_rejectsRubAmountBelowMonthlyFee() {
        assertThatThrownBy(() -> InitialSubscriptionPaymentValidator.validate(
                project, new BigDecimal("499.99"), PaymentCurrency.RUB))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Для вступления в проект по абонементу нужно оплатить полный месячный абонемент: 500 ₽.");
    }

    @Test
    void validate_acceptsRubAmountEqualToMonthlyFee() {
        assertThatCode(() -> InitialSubscriptionPaymentValidator.validate(
                project, new BigDecimal("500.00"), PaymentCurrency.RUB))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_acceptsRubAmountAboveMonthlyFee() {
        assertThatCode(() -> InitialSubscriptionPaymentValidator.validate(
                project, new BigDecimal("750.00"), PaymentCurrency.RUB))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_rejectsEurAmountBelowMonthlyFee() {
        assertThatThrownBy(() -> InitialSubscriptionPaymentValidator.validate(
                project, new BigDecimal("6.99"), PaymentCurrency.EUR))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Для вступления в проект по абонементу нужно оплатить полный месячный абонемент: 7 €.");
    }

    @Test
    void validate_acceptsEurAmountEqualToMonthlyFee() {
        assertThatCode(() -> InitialSubscriptionPaymentValidator.validate(
                project, new BigDecimal("7.00"), PaymentCurrency.EUR))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_rejectsUsdCurrency() {
        assertThatThrownBy(() -> InitialSubscriptionPaymentValidator.validate(
                project, new BigDecimal("500.00"), PaymentCurrency.USD))
                .isInstanceOf(ValidationException.class)
                .hasMessage(ValidationError.INITIAL_SUBSCRIPTION_PAYMENT_USD_NOT_SUPPORTED.getMessage());
    }
}
