package com.eliteseriespay.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectForm {

    @NotBlank(message = "Укажите название")
    private String name;

    @NotNull(message = "Укажите стоимость эпизода")
    private BigDecimal episodeCostRub;
}
