package com.eliteseriespay.web.form;

import com.eliteseriespay.domain.PaymentCurrency;
import com.eliteseriespay.domain.PaymentSource;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentForm {

    @NotNull(message = "Выберите проект")
    private Long projectId;

    @NotNull(message = "Укажите дату платежа")
    private LocalDate paymentDate;

    @NotNull(message = "Выберите источник")
    private PaymentSource source;

    @NotNull(message = "Укажите сумму")
    @DecimalMin(value = "0.01", message = "Сумма должна быть больше нуля")
    private BigDecimal amountOriginal;

    @NotNull(message = "Выберите валюту")
    private PaymentCurrency currency;

    private BigDecimal exchangeRate;

    private String comment;
}
