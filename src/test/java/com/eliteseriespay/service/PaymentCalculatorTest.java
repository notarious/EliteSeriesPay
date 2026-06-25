package com.eliteseriespay.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.eliteseriespay.domain.PaymentCurrency;
import com.eliteseriespay.domain.PaymentSource;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentCalculatorTest {

    private PaymentCalculator paymentCalculator;

    @BeforeEach
    void setUp() {
        paymentCalculator = new PaymentCalculator();
    }

    @Test
    void normalize_scalesAmount() {
        NormalizedAmounts normalized = paymentCalculator.normalize(
                new BigDecimal("100.999"), PaymentCurrency.RUB, null);

        assertThat(normalized.amountOriginal()).isEqualByComparingTo("101.00");
        assertThat(normalized.exchangeRate()).isEqualByComparingTo("1.0000");
    }

    @Test
    void normalize_forcesRubExchangeRateToOne() {
        NormalizedAmounts normalized = paymentCalculator.normalize(
                new BigDecimal("100.00"), PaymentCurrency.RUB, new BigDecimal("99.99"));

        assertThat(normalized.exchangeRate()).isEqualByComparingTo("1.0000");
    }

    @Test
    void normalize_scalesExchangeRate() {
        NormalizedAmounts normalized = paymentCalculator.normalize(
                new BigDecimal("10.00"), PaymentCurrency.USD, new BigDecimal("90.55555"));

        assertThat(normalized.exchangeRate()).isEqualByComparingTo("90.5556");
    }

    @Test
    void calculate_appliesVkDonutFee() {
        NormalizedAmounts normalized = paymentCalculator.normalize(
                new BigDecimal("1000.00"), PaymentCurrency.RUB, null);
        PaymentAmounts amounts = paymentCalculator.calculate(PaymentSource.VK_DONUT, normalized, 10);

        assertThat(amounts.amountRub()).isEqualByComparingTo("1000.00");
        assertThat(amounts.feePercent()).isEqualTo(10);
        assertThat(amounts.netAmountRub()).isEqualByComparingTo("900.00");
    }

    @Test
    void calculate_otherSourceHasZeroFee() {
        NormalizedAmounts normalized = paymentCalculator.normalize(
                new BigDecimal("1000.00"), PaymentCurrency.RUB, null);
        PaymentAmounts amounts = paymentCalculator.calculate(PaymentSource.OTHER, normalized, 10);

        assertThat(amounts.feePercent()).isZero();
        assertThat(amounts.netAmountRub()).isEqualByComparingTo("1000.00");
    }

    @Test
    void calculate_convertsForeignCurrency() {
        NormalizedAmounts normalized = paymentCalculator.normalize(
                new BigDecimal("10.00"), PaymentCurrency.USD, new BigDecimal("90.5"));
        PaymentAmounts amounts = paymentCalculator.calculate(PaymentSource.OTHER, normalized, 10);

        assertThat(amounts.amountRub()).isEqualByComparingTo("905.00");
        assertThat(amounts.netAmountRub()).isEqualByComparingTo("905.00");
    }
}
