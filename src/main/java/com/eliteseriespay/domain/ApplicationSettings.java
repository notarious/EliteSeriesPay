package com.eliteseriespay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "application_settings")
public class ApplicationSettings {

    public static final long SINGLETON_ID = 1L;
    public static final int DEFAULT_VK_DONUT_FEE_PERCENT = 10;

    @Id
    @Column(columnDefinition = "INTEGER")
    private Long id;

    @Column(name = "vk_donut_fee_percent", nullable = false)
    private int vkDonutFeePercent;

    public ApplicationSettings(int vkDonutFeePercent) {
        this.id = SINGLETON_ID;
        this.vkDonutFeePercent = vkDonutFeePercent;
    }
}
