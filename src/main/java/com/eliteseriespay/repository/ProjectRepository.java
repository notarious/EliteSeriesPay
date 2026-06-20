package com.eliteseriespay.repository;

import com.eliteseriespay.domain.MembershipStatus;
import com.eliteseriespay.domain.Project;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("""
            SELECT p FROM Project p
            WHERE NOT EXISTS (
                SELECT 1 FROM ProjectMembership m
                WHERE m.project = p
                  AND m.participant.id = :participantId
                  AND m.status = :status
            )
            ORDER BY LOWER(p.name) ASC
            """)
    List<Project> findAvailableForParticipant(@Param("participantId") Long participantId,
                                              @Param("status") MembershipStatus status);
}
