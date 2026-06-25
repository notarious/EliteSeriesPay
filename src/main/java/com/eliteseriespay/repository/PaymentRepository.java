package com.eliteseriespay.repository;

import com.eliteseriespay.domain.Payment;
import com.eliteseriespay.domain.PaymentStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
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
}
