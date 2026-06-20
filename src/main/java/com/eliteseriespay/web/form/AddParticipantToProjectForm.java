package com.eliteseriespay.web.form;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddParticipantToProjectForm {

    @NotNull(message = "Выберите проект")
    private Long projectId;
}
