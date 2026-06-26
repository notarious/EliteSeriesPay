package com.eliteseriespay.web.form;

import com.eliteseriespay.domain.BillingMode;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddParticipantToProjectForm {

    @NotNull(message = "Выберите проект")
    private Long projectId;

    @NotNull(message = "Выберите тип оплаты")
    private BillingMode billingMode;
}
