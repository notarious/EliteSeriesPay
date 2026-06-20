package com.eliteseriespay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "participants")
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INTEGER")
    private Long id;

    @Column(name = "vk_id", nullable = false, unique = true)
    private String vkId;

    @Column(nullable = false)
    private String name;

    @Column
    private String comment;

    public Participant(String vkId, String name, String comment) {
        this.vkId = vkId;
        this.name = name;
        this.comment = comment;
    }

    public void updateDetails(String vkId, String name, String comment) {
        this.vkId = vkId;
        this.name = name;
        this.comment = comment;
    }
}
