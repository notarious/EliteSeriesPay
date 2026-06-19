package com.eliteseriespay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INTEGER")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "episode_cost_rub", nullable = false)
    private BigDecimal episodeCostRub;

    protected Project() {
    }

    public Project(String name, BigDecimal episodeCostRub) {
        this.name = name;
        this.episodeCostRub = episodeCostRub;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getEpisodeCostRub() {
        return episodeCostRub;
    }

    public void updateDetails(String name, BigDecimal episodeCostRub) {
        this.name = name;
        this.episodeCostRub = episodeCostRub;
    }
}
