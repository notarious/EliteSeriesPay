package com.eliteseriespay.support;

import com.eliteseriespay.domain.BillingMode;
import com.eliteseriespay.domain.MembershipStatus;
import com.eliteseriespay.domain.Participant;
import com.eliteseriespay.domain.Payment;
import com.eliteseriespay.domain.PaymentCurrency;
import com.eliteseriespay.domain.PaymentSource;
import com.eliteseriespay.domain.PaymentStatus;
import com.eliteseriespay.domain.Project;
import com.eliteseriespay.domain.ProjectMembership;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.test.util.ReflectionTestUtils;

public final class TestEntities {

    private TestEntities() {
    }

    public static void setId(Object entity, long id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }

    public static Project project(long id, String name, BigDecimal episodeCostRub) {
        return project(id, name, episodeCostRub, episodeCostRub, BigDecimal.ONE);
    }

    public static Project project(long id,
                                  String name,
                                  BigDecimal episodeCostRub,
                                  BigDecimal monthlyFeeRub,
                                  BigDecimal monthlyFeeEur) {
        Project project = new Project(name, episodeCostRub, monthlyFeeRub, monthlyFeeEur);
        setId(project, id);
        return project;
    }

    public static Participant participant(long id, String vkId, String name, String comment) {
        Participant participant = new Participant(vkId, name, comment);
        setId(participant, id);
        return participant;
    }

    public static ProjectMembership membership(Project project,
                                               Participant participant,
                                               MembershipStatus status) {
        return membership(project, participant, status, BillingMode.SUBSCRIPTION);
    }

    public static ProjectMembership membership(Project project,
                                               Participant participant,
                                               MembershipStatus status,
                                               BillingMode billingMode) {
        return new ProjectMembership(project, participant, status, billingMode);
    }

    public static Payment payment(long id,
                                  Participant participant,
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
        return payment(id, participant, project, paymentDate, source, amountOriginal, currency,
                exchangeRate, amountRub, feePercent, netAmountRub, comment, PaymentStatus.ACTIVE);
    }

    public static Payment payment(long id,
                                  Participant participant,
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
        Payment payment = new Payment(
                participant, project, paymentDate, source, amountOriginal, currency,
                exchangeRate, amountRub, feePercent, netAmountRub, comment, status);
        setId(payment, id);
        return payment;
    }
}
