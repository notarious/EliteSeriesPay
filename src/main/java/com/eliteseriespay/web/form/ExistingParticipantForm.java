package com.eliteseriespay.web.form;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExistingParticipantForm {

    @NotNull(message = "Выберите участника")
    private Long participantId;
}
