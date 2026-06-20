package com.eliteseriespay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "project_memberships",
        uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "participant_id"})
)
public class ProjectMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INTEGER")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipStatus status;

    public ProjectMembership(Project project, Participant participant, MembershipStatus status) {
        this.project = project;
        this.participant = participant;
        this.status = status;
    }

    public void markActive() {
        this.status = MembershipStatus.ACTIVE;
    }

    public void markLeft() {
        this.status = MembershipStatus.LEFT;
    }
}
