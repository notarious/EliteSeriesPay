package com.eliteseriespay.domain;

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
import com.eliteseriespay.domain.converter.LocalDateStringConverter;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INTEGER")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    @ManyToOne(optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Convert(converter = LocalDateStringConverter.class)
    @Column(name = "payment_date", nullable = false, columnDefinition = "TEXT")
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentSource source;

    @Column(name = "amount_original", nullable = false)
    private BigDecimal amountOriginal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentCurrency currency;

    @Column(name = "exchange_rate", nullable = false)
    private BigDecimal exchangeRate;

    @Column(name = "amount_rub", nullable = false)
    private BigDecimal amountRub;

    @Column(name = "fee_percent", nullable = false)
    private int feePercent;

    @Column(name = "net_amount_rub", nullable = false)
    private BigDecimal netAmountRub;

    @Column
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    public Payment(Participant participant,
                   Project project,
                   LocalDate paymentDate,
                   PaymentSource source,
                   BigDecimal amountOriginal,
                   PaymentCurrency currency,
                   BigDecimal exchangeRate,
                   BigDecimal amountRub,
                   int feePercent,
                   BigDecimal netAmountRub,
                   String comment) {
        this(participant, project, paymentDate, source, amountOriginal, currency, exchangeRate,
                amountRub, feePercent, netAmountRub, comment, PaymentStatus.ACTIVE);
    }

    public Payment(Participant participant,
                   Project project,
                   LocalDate paymentDate,
                   PaymentSource source,
                   BigDecimal amountOriginal,
                   PaymentCurrency currency,
                   BigDecimal exchangeRate,
                   BigDecimal amountRub,
                   int feePercent,
                   BigDecimal netAmountRub,
                   String comment,
                   PaymentStatus status) {
        this.participant = participant;
        this.project = project;
        this.paymentDate = paymentDate;
        this.source = source;
        this.amountOriginal = amountOriginal;
        this.currency = currency;
        this.exchangeRate = exchangeRate;
        this.amountRub = amountRub;
        this.feePercent = feePercent;
        this.netAmountRub = netAmountRub;
        this.comment = comment;
        this.status = status;
    }

    public void update(Project project,
                       LocalDate paymentDate,
                       PaymentSource source,
                       BigDecimal amountOriginal,
                       PaymentCurrency currency,
                       BigDecimal exchangeRate,
                       BigDecimal amountRub,
                       int feePercent,
                       BigDecimal netAmountRub,
                       String comment) {
        this.project = project;
        this.paymentDate = paymentDate;
        this.source = source;
        this.amountOriginal = amountOriginal;
        this.currency = currency;
        this.exchangeRate = exchangeRate;
        this.amountRub = amountRub;
        this.feePercent = feePercent;
        this.netAmountRub = netAmountRub;
        this.comment = comment;
    }

    public void voidPayment() {
        this.status = PaymentStatus.VOIDED;
    }
}
