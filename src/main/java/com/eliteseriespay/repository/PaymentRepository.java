package com.eliteseriespay.repository;

import com.eliteseriespay.domain.Payment;
import com.eliteseriespay.domain.PaymentSource;
import com.eliteseriespay.domain.PaymentStatus;
import com.eliteseriespay.domain.Project;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("""
            SELECT p FROM Payment p
            JOIN FETCH p.project
            WHERE p.participant.id = :participantId
            ORDER BY p.paymentDate DESC, p.id DESC
            """)
    List<Payment> findByParticipantIdOrderByPaymentDateDescIdDesc(@Param("participantId") Long participantId);

    boolean existsByParticipant_Id(Long participantId);

    @EntityGraph(attributePaths = "project")
    @Query(value = """
            SELECT p FROM Payment p
            WHERE p.participant.id = :participantId
            AND (:projectId IS NULL OR p.project.id = :projectId)
            AND (:source IS NULL OR p.source = :source)
            AND (:status IS NULL OR p.status = :status)
            AND (:dateFrom IS NULL OR p.paymentDate >= :dateFrom)
            AND (:dateTo IS NULL OR p.paymentDate <= :dateTo)
            """,
            countQuery = """
            SELECT COUNT(p) FROM Payment p
            WHERE p.participant.id = :participantId
            AND (:projectId IS NULL OR p.project.id = :projectId)
            AND (:source IS NULL OR p.source = :source)
            AND (:status IS NULL OR p.status = :status)
            AND (:dateFrom IS NULL OR p.paymentDate >= :dateFrom)
            AND (:dateTo IS NULL OR p.paymentDate <= :dateTo)
            """)
    Page<Payment> findParticipantPaymentHistory(@Param("participantId") Long participantId,
                                                @Param("projectId") Long projectId,
                                                @Param("source") PaymentSource source,
                                                @Param("status") PaymentStatus status,
                                                @Param("dateFrom") LocalDate dateFrom,
                                                @Param("dateTo") LocalDate dateTo,
                                                Pageable pageable);

    @Query("""
            SELECT DISTINCT p.project FROM Payment p
            WHERE p.participant.id = :participantId
            ORDER BY p.project.name ASC
            """)
    List<Project> findDistinctProjectsByParticipantIdOrderByNameAsc(@Param("participantId") Long participantId);

    @Query("""
            SELECT p FROM Payment p
            JOIN FETCH p.project
            WHERE p.id = :paymentId AND p.participant.id = :participantId
            """)
    Optional<Payment> findByIdAndParticipantId(@Param("paymentId") Long paymentId,
                                               @Param("participantId") Long participantId);

    Optional<Payment> findFirstByParticipant_IdAndStatusOrderByPaymentDateDescIdDesc(
            Long participantId, PaymentStatus status);

    Optional<Payment> findFirstByParticipant_IdAndProject_IdAndStatusOrderByPaymentDateDescIdDesc(
            Long participantId, Long projectId, PaymentStatus status);

    @Query("""
            SELECT COALESCE(SUM(p.netAmountRub), 0) FROM Payment p
            WHERE p.participant.id = :participantId
            AND p.status = com.eliteseriespay.domain.PaymentStatus.ACTIVE
            """)
    BigDecimal sumActiveNetAmountRubByParticipantId(@Param("participantId") Long participantId);

    @Query("""
            SELECT p FROM Payment p
            JOIN FETCH p.participant
            WHERE p.project.id = :projectId
            AND p.status = com.eliteseriespay.domain.PaymentStatus.ACTIVE
            AND NOT EXISTS (
                SELECT 1 FROM Payment newer
                WHERE newer.project.id = :projectId
                AND newer.participant.id = p.participant.id
                AND newer.status = com.eliteseriespay.domain.PaymentStatus.ACTIVE
                AND (newer.paymentDate > p.paymentDate
                     OR (newer.paymentDate = p.paymentDate AND newer.id > p.id))
            )
            """)
    List<Payment> findLatestActivePaymentsByProjectId(@Param("projectId") Long projectId);

    @Query("""
            SELECT p FROM Payment p
            WHERE p.project.id = :projectId
            AND p.participant.id = :participantId
            AND p.status = com.eliteseriespay.domain.PaymentStatus.ACTIVE
            ORDER BY p.paymentDate ASC, p.id ASC
            """)
    List<Payment> findActivePaymentsByProjectAndParticipant(@Param("projectId") Long projectId,
                                                            @Param("participantId") Long participantId);
}
