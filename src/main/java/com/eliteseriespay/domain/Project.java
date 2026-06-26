package com.eliteseriespay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Column(name = "monthly_fee_rub", nullable = false)
    private BigDecimal monthlyFeeRub;

    @Column(name = "monthly_fee_eur", nullable = false)
    private BigDecimal monthlyFeeEur;

    public Project(String name, BigDecimal episodeCostRub, BigDecimal monthlyFeeRub, BigDecimal monthlyFeeEur) {
        this.name = name;
        this.episodeCostRub = episodeCostRub;
        this.monthlyFeeRub = monthlyFeeRub;
        this.monthlyFeeEur = monthlyFeeEur;
    }

    public void updateDetails(String name,
                              BigDecimal episodeCostRub,
                              BigDecimal monthlyFeeRub,
                              BigDecimal monthlyFeeEur) {
        this.name = name;
        this.episodeCostRub = episodeCostRub;
        this.monthlyFeeRub = monthlyFeeRub;
        this.monthlyFeeEur = monthlyFeeEur;
    }
}
