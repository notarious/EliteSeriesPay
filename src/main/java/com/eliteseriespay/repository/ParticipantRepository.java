package com.eliteseriespay.repository;

import com.eliteseriespay.domain.MembershipStatus;
import com.eliteseriespay.domain.Participant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    Optional<Participant> findByVkId(String vkId);

    List<Participant> findAllByOrderByNameAsc();

    Page<Participant> findAllByOrderByNameAsc(Pageable pageable);

    @Query(value = """
            SELECT p FROM Participant p
            WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(p.vkId) LIKE LOWER(CONCAT('%', :query, '%'))
            """,
            countQuery = """
            SELECT COUNT(p) FROM Participant p
            WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(p.vkId) LIKE LOWER(CONCAT('%', :query, '%'))
            """)
    Page<Participant> searchByNameOrVkIdIgnoreCase(@Param("query") String query, Pageable pageable);

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
