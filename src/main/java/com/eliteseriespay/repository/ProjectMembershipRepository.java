package com.eliteseriespay.repository;

import com.eliteseriespay.domain.MembershipStatus;
import com.eliteseriespay.domain.ProjectMembership;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectMembershipRepository extends JpaRepository<ProjectMembership, Long> {

    Optional<ProjectMembership> findByProject_IdAndParticipant_Id(Long projectId, Long participantId);

    @Query("""
            SELECT m FROM ProjectMembership m
            JOIN FETCH m.participant p
            WHERE m.project.id = :projectId AND m.status = :status
            ORDER BY p.name ASC
            """)
    List<ProjectMembership> findByProjectIdAndStatus(@Param("projectId") Long projectId,
                                                       @Param("status") MembershipStatus status);
}
