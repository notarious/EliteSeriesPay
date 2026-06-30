package com.eliteseriespay.format;

import static org.assertj.core.api.Assertions.assertThat;

import com.eliteseriespay.domain.PaymentCurrency;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AmountFormatterTest {

    @Test
    void formatWithCurrency_formatsRubAmountLikeUi() {
        assertThat(AmountFormatter.formatWithCurrency(new BigDecimal("300.00"), PaymentCurrency.RUB))
                .isEqualTo("300,00 ₽");
    }

    @Test
    void formatWithCurrency_formatsRubAmountWithGrouping() {
        assertThat(AmountFormatter.formatWithCurrency(new BigDecimal("1500.00"), PaymentCurrency.RUB))
                .isEqualTo("1 500,00 ₽");
    }

    @Test
    void formatWithCurrency_formatsEurAmount() {
        assertThat(AmountFormatter.formatWithCurrency(new BigDecimal("8.00"), PaymentCurrency.EUR))
                .isEqualTo("8,00 €");
    }
}
