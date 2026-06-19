package com.eliteseriespay.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class ProjectForm {

    @NotBlank(message = "Укажите название")
    private String name;

    @NotNull(message = "Укажите стоимость эпизода")
    private BigDecimal episodeCostRub;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getEpisodeCostRub() {
        return episodeCostRub;
    }

    public void setEpisodeCostRub(BigDecimal episodeCostRub) {
        this.episodeCostRub = episodeCostRub;
    }
}
