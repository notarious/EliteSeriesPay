package com.eliteseriespay.payment.history;

import static org.assertj.core.api.Assertions.assertThat;

import com.eliteseriespay.domain.BillingMode;
import com.eliteseriespay.domain.PaymentCurrency;
import com.eliteseriespay.domain.PaymentSource;
import com.eliteseriespay.domain.PaymentStatus;
import com.eliteseriespay.domain.Project;
import com.eliteseriespay.support.TestEntities;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentHistoryFormatterTest {

    private PaymentHistoryFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new PaymentHistoryFormatter();
    }

    @Test
    void toRowView_formatsPaymentFields() {
        var participant = TestEntities.participant(10L, "12345", "Ivan", null);
        Project project = TestEntities.project(1L, "Series", new BigDecimal("1000.00"));
        var payment = TestEntities.payment(
                1L, participant, project, LocalDate.of(2026, 6, 15), PaymentSource.VK_DONUT,
                new BigDecimal("100.00"), PaymentCurrency.RUB, new BigDecimal("1.0000"),
                new BigDecimal("100.00"), 10, new BigDecimal("90.00"), "note");

        PaymentHistoryRowView row = formatter.toRowView(payment, BillingMode.SUBSCRIPTION);

        assertThat(row.paymentDate()).isEqualTo("15.06.2026");
        assertThat(row.participantName()).isEqualTo("Ivan");
        assertThat(row.billingModeLabel()).isEqualTo("Абонемент");
        assertThat(row.sourceLabel()).isEqualTo("VK Donut (-10%)");
        assertThat(row.currencyLabel()).isEqualTo("RUB");
        assertThat(row.exchangeRate()).isEqualTo("—");
        assertThat(row.statusLabel()).isEqualTo("Активный");
        assertThat(row.comment()).isEqualTo("note");
        assertThat(row.editable()).isTrue();
    }

    @Test
    void sourceLabel_mapsManualToOther() {
        assertThat(formatter.sourceLabel(PaymentSource.MANUAL)).isEqualTo("Другое");
    }

    @Test
    void toRowView_marksVoidedPaymentAsNotEditable() {
        var participant = TestEntities.participant(10L, "12345", "Ivan", null);
        Project project = TestEntities.project(1L, "Series", new BigDecimal("1000.00"));
        var payment = TestEntities.payment(
                1L, participant, project, LocalDate.of(2026, 6, 15), PaymentSource.MANUAL,
                new BigDecimal("100.00"), PaymentCurrency.EUR, new BigDecimal("90.0000"),
                new BigDecimal("9000.00"), 0, new BigDecimal("9000.00"), null, PaymentStatus.VOIDED);

        PaymentHistoryRowView row = formatter.toRowView(payment, BillingMode.PACKAGE);

        assertThat(row.billingModeLabel()).isEqualTo("Пакет");
        assertThat(row.voided()).isTrue();
        assertThat(row.editable()).isFalse();
        assertThat(row.statusLabel()).isEqualTo("Аннулирован");
        assertThat(row.exchangeRate()).isEqualTo("90,0000");
    }
}
