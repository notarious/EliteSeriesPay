package com.eliteseriespay.repository;

import com.eliteseriespay.domain.Payment;
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

    Optional<Payment> findTopByParticipantIdOrderByPaymentDateDescIdDesc(Long participantId);

    Optional<Payment> findTopByParticipantIdAndProjectIdOrderByPaymentDateDescIdDesc(
            Long participantId, Long projectId);

    @Query("""
            SELECT COALESCE(SUM(p.netAmountRub), 0) FROM Payment p
            WHERE p.participant.id = :participantId
            """)
    BigDecimal sumNetAmountRubByParticipantId(@Param("participantId") Long participantId);

    @Query("""
            SELECT p FROM Payment p
            JOIN FETCH p.participant
            WHERE p.project.id = :projectId
            AND NOT EXISTS (
                SELECT 1 FROM Payment newer
                WHERE newer.project.id = :projectId
                AND newer.participant.id = p.participant.id
                AND (newer.paymentDate > p.paymentDate
                     OR (newer.paymentDate = p.paymentDate AND newer.id > p.id))
            )
            """)
    List<Payment> findLatestPaymentsByProjectId(@Param("projectId") Long projectId);
}
