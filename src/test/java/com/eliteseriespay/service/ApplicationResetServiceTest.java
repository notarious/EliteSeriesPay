package com.eliteseriespay.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.eliteseriespay.config.ApplicationDataDirectory;
import com.eliteseriespay.domain.Participant;
import com.eliteseriespay.domain.Payment;
import com.eliteseriespay.domain.PaymentCurrency;
import com.eliteseriespay.domain.PaymentSource;
import com.eliteseriespay.domain.Project;
import com.eliteseriespay.domain.ProjectMembership;
import com.eliteseriespay.domain.MembershipStatus;
import com.eliteseriespay.domain.ApplicationSettings;
import com.eliteseriespay.domain.BillingMode;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import com.eliteseriespay.repository.ApplicationSettingsRepository;
import com.eliteseriespay.repository.ParticipantRepository;
import com.eliteseriespay.repository.PaymentRepository;
import com.eliteseriespay.repository.ProjectMembershipRepository;
import com.eliteseriespay.repository.ProjectRepository;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {
        "eliteseriespay.database-backup.startup-enabled=false"
})
class ApplicationResetServiceTest {

    @TempDir
    static Path tempDir;

    private static Path databasePath;
    private static Path backupDirectory;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        databasePath = tempDir.resolve("application-reset-test.db");
        backupDirectory = tempDir.resolve("backups");
        registry.add("spring.datasource.url", () -> ApplicationDataDirectory.toJdbcSqliteFileUrl(databasePath));
        registry.add("eliteseriespay.database-backup.database-path", () -> databasePath.toString());
        registry.add("eliteseriespay.database-backup.backup-directory", () -> backupDirectory.toString());
    }

    @Autowired
    private ApplicationResetService applicationResetService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ProjectMembershipRepository projectMembershipRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private ApplicationSettingsRepository applicationSettingsRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterAll
    static void releaseDatabaseResources(
            @Autowired EntityManagerFactory entityManagerFactory, @Autowired DataSource dataSource) {
        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            entityManagerFactory.close();
        }
        if (dataSource instanceof HikariDataSource hikariDataSource) {
            hikariDataSource.close();
        }
    }

    @BeforeEach
    void seedData() throws Exception {
        Files.createDirectories(backupDirectory);
        Files.writeString(backupDirectory.resolve("eliteseriespay-2026-06-26-12-00-00.db"), "backup-content");

        if (projectRepository.count() == 0) {
            Project project = projectRepository.save(
                    new Project("Series", new BigDecimal("1000.00"), new BigDecimal("1000.00"), BigDecimal.ONE));
            Participant participant = participantRepository.save(new Participant("12345", "Ivan", null));
            projectMembershipRepository.save(
                    new ProjectMembership(project, participant, MembershipStatus.ACTIVE, BillingMode.SUBSCRIPTION));
            paymentRepository.save(new Payment(
                    participant,
                    project,
                    LocalDate.of(2026, 6, 1),
                    PaymentSource.VK_DONUT,
                    new BigDecimal("100.00"),
                    PaymentCurrency.RUB,
                    BigDecimal.ONE,
                    new BigDecimal("100.00"),
                    10,
                    new BigDecimal("90.00"),
                    null));
        }

        if (applicationSettingsRepository.count() == 0) {
            applicationSettingsRepository.save(new ApplicationSettings(10));
        }
    }

    @Test
    void resetAllData_clearsUserTables() {
        assertThat(paymentRepository.count()).isPositive();
        assertThat(projectMembershipRepository.count()).isPositive();
        assertThat(projectRepository.count()).isPositive();
        assertThat(participantRepository.count()).isPositive();
        assertThat(applicationSettingsRepository.count()).isPositive();

        applicationResetService.resetAllData();

        assertThat(paymentRepository.count()).isZero();
        assertThat(projectMembershipRepository.count()).isZero();
        assertThat(projectRepository.count()).isZero();
        assertThat(participantRepository.count()).isZero();
        assertThat(applicationSettingsRepository.count()).isZero();
    }

    @Test
    void resetAllData_preservesFlywayMetadata() {
        Integer migrationCountBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history", Integer.class);
        assertThat(migrationCountBefore).isPositive();

        applicationResetService.resetAllData();

        Integer migrationCountAfter = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history", Integer.class);
        assertThat(migrationCountAfter).isEqualTo(migrationCountBefore);
    }

    @Test
    void resetAllData_preservesExistingBackups() throws Exception {
        Path backupFile = backupDirectory.resolve("eliteseriespay-2026-06-26-12-00-00.db");
        assertThat(backupFile).exists();

        applicationResetService.resetAllData();

        assertThat(backupFile).exists();
        assertThat(Files.readString(backupFile)).isEqualTo("backup-content");
    }

    @Test
    void resetAllData_allowsApplicationToKeepRunning() {
        applicationResetService.resetAllData();

        assertThat(projectRepository.findAll()).isEmpty();
        assertThat(participantRepository.findAll()).isEmpty();
        assertThat(paymentRepository.findAll()).isEmpty();
    }
}
