package com.eliteseriespay.repository;

import com.eliteseriespay.domain.MembershipStatus;
import com.eliteseriespay.domain.Participant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    Optional<Participant> findByVkId(String vkId);

    List<Participant> findAllByOrderByNameAsc();

    @Query("""
            SELECT p FROM Participant p
            WHERE NOT EXISTS (
                SELECT 1 FROM ProjectMembership m
                WHERE m.participant = p
                  AND m.project.id = :projectId
                  AND m.status = :status
            )
            ORDER BY p.name ASC
            """)
    List<Participant> findAvailableForProject(@Param("projectId") Long projectId,
                                              @Param("status") MembershipStatus status);
}
