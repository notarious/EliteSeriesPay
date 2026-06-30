package com.eliteseriespay.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.eliteseriespay.EliteSeriesPayApplication;
import com.eliteseriespay.domain.ApplicationSettings;
import com.eliteseriespay.domain.BillingMode;
import com.eliteseriespay.domain.MembershipStatus;
import com.eliteseriespay.domain.Participant;
import com.eliteseriespay.domain.Payment;
import com.eliteseriespay.domain.PaymentCurrency;
import com.eliteseriespay.domain.PaymentSource;
import com.eliteseriespay.domain.Project;
import com.eliteseriespay.domain.ProjectMembership;
import com.eliteseriespay.repository.ApplicationSettingsRepository;
import com.eliteseriespay.repository.ParticipantRepository;
import com.eliteseriespay.repository.PaymentRepository;
import com.eliteseriespay.repository.ProjectMembershipRepository;
import com.eliteseriespay.repository.ProjectRepository;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;

class LegacyDatabaseLocationUpgradeIntegrationTest {

    private static final int CUSTOM_VK_DONUT_FEE_PERCENT = 15;

    @Test
    void upgradeFromLegacyDatabaseLocation_preservesAllData(@TempDir Path userDataDir) throws Exception {
        Path legacyDatabase = ApplicationDataDirectory.legacyDatabasePath(userDataDir);
        Path migratedDatabase = ApplicationDataDirectory.databasePath(userDataDir);

        LegacyDataSnapshot expected = populateLegacyDatabase(userDataDir, legacyDatabase);

        assertThat(Files.exists(legacyDatabase)).isTrue();
        assertThat(Files.exists(migratedDatabase)).isFalse();

        MockEnvironment upgradeEnvironment = new MockEnvironment()
                .withProperty(ApplicationDataDirectory.DATA_DIR_PROPERTY, formatPath(userDataDir));
        new ApplicationDataDirectoryEnvironmentPostProcessor()
                .postProcessEnvironment(upgradeEnvironment, new SpringApplication());

        assertThat(Files.exists(legacyDatabase)).isFalse();
        assertThat(Files.exists(migratedDatabase)).isTrue();

        LegacyDataSnapshot actual = startApplicationWithResolvedUpgradeConfiguration(upgradeEnvironment);

        assertThat(actual).isEqualTo(expected);
    }

    private LegacyDataSnapshot populateLegacyDatabase(Path userDataDir, Path legacyDatabase) {
        LegacyDataSnapshot[] snapshot = new LegacyDataSnapshot[1];
        new ApplicationContextRunner()
                .withUserConfiguration(EliteSeriesPayApplication.class)
                .withPropertyValues(
                        "spring.main.web-application-type=none",
                        "eliteseriespay.database-backup.startup-enabled=false",
                        "spring.datasource.url=" + ApplicationDataDirectory.toJdbcSqliteFileUrl(legacyDatabase),
                        "eliteseriespay.database-backup.database-path=" + formatPath(legacyDatabase),
                        "eliteseriespay.database-backup.backup-directory=" + formatPath(userDataDir.resolve("backups")))
                .run(context -> snapshot[0] = seedAndCaptureSnapshot(context));
        return snapshot[0];
    }

    private LegacyDataSnapshot startApplicationWithResolvedUpgradeConfiguration(MockEnvironment upgradeEnvironment) {
        LegacyDataSnapshot[] snapshot = new LegacyDataSnapshot[1];
        new ApplicationContextRunner()
                .withUserConfiguration(EliteSeriesPayApplication.class)
                .withPropertyValues(
                        "spring.main.web-application-type=none",
                        "eliteseriespay.database-backup.startup-enabled=false",
                        "spring.datasource.url=" + upgradeEnvironment.getRequiredProperty("spring.datasource.url"),
                        "eliteseriespay.database-backup.database-path="
                                + upgradeEnvironment.getRequiredProperty("eliteseriespay.database-backup.database-path"),
                        "eliteseriespay.database-backup.backup-directory="
                                + upgradeEnvironment.getRequiredProperty("eliteseriespay.database-backup.backup-directory"),
                        ApplicationDataDirectory.DATA_DIR_PROPERTY + "="
                                + upgradeEnvironment.getRequiredProperty(ApplicationDataDirectory.DATA_DIR_PROPERTY))
                .run(context -> snapshot[0] = captureSnapshot(context));
        return snapshot[0];
    }

    private static LegacyDataSnapshot seedAndCaptureSnapshot(ConfigurableApplicationContext context) {
        ProjectRepository projectRepository = context.getBean(ProjectRepository.class);
        ParticipantRepository participantRepository = context.getBean(ParticipantRepository.class);
        ProjectMembershipRepository projectMembershipRepository = context.getBean(ProjectMembershipRepository.class);
        PaymentRepository paymentRepository = context.getBean(PaymentRepository.class);
        ApplicationSettingsRepository applicationSettingsRepository = context.getBean(ApplicationSettingsRepository.class);

        Project project = projectRepository.save(new Project(
                "Legacy Series",
                new BigDecimal("4500.00"),
                new BigDecimal("500.00"),
                new BigDecimal("8.50")));
        Participant participant = participantRepository.save(new Participant("987654321", "Maria", "legacy comment"));

        ProjectMembership membership = projectMembershipRepository.save(new ProjectMembership(
                project, participant, MembershipStatus.ACTIVE, BillingMode.SUBSCRIPTION));
        membership.updateBilling(
                YearMonth.of(2026, 8),
                new BigDecimal("250.00"),
                PaymentCurrency.RUB);
        projectMembershipRepository.save(membership);

        paymentRepository.save(new Payment(
                participant,
                project,
                LocalDate.of(2026, 6, 15),
                PaymentSource.VK_DONUT,
                new BigDecimal("500.00"),
                PaymentCurrency.RUB,
                BigDecimal.ONE,
                new BigDecimal("500.00"),
                CUSTOM_VK_DONUT_FEE_PERCENT,
                new BigDecimal("450.00"),
                null));

        if (applicationSettingsRepository.count() == 0) {
            applicationSettingsRepository.save(new ApplicationSettings(CUSTOM_VK_DONUT_FEE_PERCENT));
        }

        return captureSnapshot(context);
    }

    private static LegacyDataSnapshot captureSnapshot(ConfigurableApplicationContext context) {
        ProjectRepository projectRepository = context.getBean(ProjectRepository.class);
        ParticipantRepository participantRepository = context.getBean(ParticipantRepository.class);
        ProjectMembershipRepository projectMembershipRepository = context.getBean(ProjectMembershipRepository.class);
        PaymentRepository paymentRepository = context.getBean(PaymentRepository.class);
        ApplicationSettingsRepository applicationSettingsRepository = context.getBean(ApplicationSettingsRepository.class);
        JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

        Project project = projectRepository.findAll().getFirst();
        Participant participant = participantRepository.findAll().getFirst();
        ProjectMembership membership = projectMembershipRepository.findAll().getFirst();
        Payment payment = paymentRepository.findAll().getFirst();
        ApplicationSettings settings = applicationSettingsRepository.findById(ApplicationSettings.SINGLETON_ID).orElseThrow();
        Integer flywayMigrationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history", Integer.class);

        return new LegacyDataSnapshot(
                project.getId(),
                project.getName(),
                project.getEpisodeCostRub(),
                project.getMonthlyFeeRub(),
                project.getMonthlyFeeEur(),
                participant.getId(),
                participant.getVkId(),
                participant.getName(),
                participant.getComment(),
                membership.getId(),
                membership.getStatus(),
                membership.getBillingMode(),
                membership.getPaidUntilMonth(),
                membership.getPartialPaymentAmount(),
                membership.getPartialPaymentCurrency(),
                payment.getId(),
                payment.getPaymentDate(),
                payment.getSource(),
                payment.getAmountOriginal(),
                payment.getCurrency(),
                payment.getFeePercent(),
                payment.getNetAmountRub(),
                settings.getVkDonutFeePercent(),
                projectRepository.count(),
                participantRepository.count(),
                projectMembershipRepository.count(),
                paymentRepository.count(),
                flywayMigrationCount);
    }

    private static String formatPath(Path path) {
        return path.toAbsolutePath().normalize().toString().replace('\\', '/');
    }

    private record LegacyDataSnapshot(
            Long projectId,
            String projectName,
            BigDecimal episodeCostRub,
            BigDecimal monthlyFeeRub,
            BigDecimal monthlyFeeEur,
            Long participantId,
            String participantVkId,
            String participantName,
            String participantComment,
            Long membershipId,
            MembershipStatus membershipStatus,
            BillingMode billingMode,
            YearMonth paidUntilMonth,
            BigDecimal partialPaymentAmount,
            PaymentCurrency partialPaymentCurrency,
            Long paymentId,
            LocalDate paymentDate,
            PaymentSource paymentSource,
            BigDecimal paymentAmountOriginal,
            PaymentCurrency paymentCurrency,
            int paymentFeePercent,
            BigDecimal paymentNetAmountRub,
            int vkDonutFeePercent,
            long projectCount,
            long participantCount,
            long membershipCount,
            long paymentCount,
            int flywayMigrationCount) {
    }
}
