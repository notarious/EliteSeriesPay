package com.eliteseriespay.web.form;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParticipantEditForm {

    @NotBlank(message = "Укажите VK ID")
    private String vkId;

    @NotBlank(message = "Укажите имя")
    private String name;

    private String comment;
}
