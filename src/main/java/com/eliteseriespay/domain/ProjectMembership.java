package com.eliteseriespay.domain;

import com.eliteseriespay.domain.converter.YearMonthStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import java.math.BigDecimal;
import java.time.YearMonth;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_mode", nullable = false)
    private BillingMode billingMode;

    @Convert(converter = YearMonthStringConverter.class)
    @Column(name = "paid_until_month", columnDefinition = "TEXT")
    private YearMonth paidUntilMonth;

    @Column(name = "partial_payment_amount")
    private BigDecimal partialPaymentAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "partial_payment_currency")
    private PaymentCurrency partialPaymentCurrency;

    public ProjectMembership(Project project, Participant participant, MembershipStatus status, BillingMode billingMode) {
        this.project = project;
        this.participant = participant;
        this.status = status;
        this.billingMode = billingMode;
    }

    public void markActive() {
        this.status = MembershipStatus.ACTIVE;
    }

    public void restoreActive(BillingMode billingMode) {
        this.status = MembershipStatus.ACTIVE;
        this.billingMode = billingMode;
    }

    public void markLeft() {
        this.status = MembershipStatus.LEFT;
    }

    public void updateBilling(YearMonth paidUntilMonth,
                              BigDecimal partialPaymentAmount,
                              PaymentCurrency partialPaymentCurrency) {
        this.paidUntilMonth = paidUntilMonth;
        this.partialPaymentAmount = partialPaymentAmount;
        this.partialPaymentCurrency = partialPaymentCurrency;
    }
}
