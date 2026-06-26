package com.eliteseriespay.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PaymentSourceTest {

    @Test
    void displayNames_matchUiLabels() {
        assertThat(PaymentSource.VK_DONUT.getDisplayName()).isEqualTo("VK Donut (-10%)");
        assertThat(PaymentSource.MANUAL.getDisplayName()).isEqualTo("Вручную");
    }
}
