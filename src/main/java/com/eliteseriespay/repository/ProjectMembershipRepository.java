package com.eliteseriespay.repository;

import com.eliteseriespay.domain.MembershipStatus;
import com.eliteseriespay.domain.ProjectMembership;
import java.util.Collection;
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

    @Query("""
            SELECT m FROM ProjectMembership m
            JOIN FETCH m.project p
            WHERE m.participant.id = :participantId AND m.status = :status
            ORDER BY p.name ASC
            """)
    List<ProjectMembership> findByParticipantIdAndStatus(@Param("participantId") Long participantId,
                                                           @Param("status") MembershipStatus status);

    @Query("""
            SELECT m.participant.id, COUNT(m)
            FROM ProjectMembership m
            WHERE m.status = :status
            GROUP BY m.participant.id
            """)
    List<Object[]> countGroupedByParticipantId(@Param("status") MembershipStatus status);

    @Query("""
            SELECT m.participant.id, COUNT(m)
            FROM ProjectMembership m
            WHERE m.status = :status AND m.participant.id IN :participantIds
            GROUP BY m.participant.id
            """)
    List<Object[]> countGroupedByParticipantIdIn(@Param("status") MembershipStatus status,
                                                   @Param("participantIds") Collection<Long> participantIds);
}
