package com.eliteseriespay.web.form;

import com.eliteseriespay.domain.BillingMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParticipantForm {

    @NotBlank(message = "Укажите VK ID")
    private String vkId;

    @NotBlank(message = "Укажите имя")
    private String name;

    private String comment;

    @NotNull(message = "Выберите тип оплаты")
    private BillingMode billingMode;
}
