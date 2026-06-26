package com.eliteseriespay.web.form;

import com.eliteseriespay.domain.BillingMode;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExistingParticipantForm {

    @NotNull(message = "Выберите участника")
    private Long participantId;

    @NotNull(message = "Выберите тип оплаты")
    private BillingMode billingMode;
}
