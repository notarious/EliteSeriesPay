package com.eliteseriespay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eliteseriespay.domain.Project;
import com.eliteseriespay.exception.NotFoundException;
import com.eliteseriespay.exception.ValidationException;
import com.eliteseriespay.repository.ProjectRepository;
import com.eliteseriespay.support.TestEntities;
import com.eliteseriespay.validation.ValidationError;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void create_trimsNameAndSavesProject() {
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Project created = projectService.create(
                "  My project  ", new BigDecimal("1500.50"), new BigDecimal("500.00"), new BigDecimal("5.00"));

        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(projectCaptor.capture());

        assertThat(projectCaptor.getValue().getName()).isEqualTo("My project");
        assertThat(projectCaptor.getValue().getEpisodeCostRub()).isEqualByComparingTo("1500.50");
        assertThat(created).isEqualTo(projectCaptor.getValue());
    }

    @Test
    void create_rejectsBlankName() {
        assertThatThrownBy(() -> projectService.create("  ", new BigDecimal("100.00"), new BigDecimal("50.00"), new BigDecimal("5.00")))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getError())
                        .isEqualTo(ValidationError.PROJECT_NAME_REQUIRED));
    }

    @Test
    void create_rejectsMissingEpisodeCost() {
        assertThatThrownBy(() -> projectService.create("My project", null, new BigDecimal("50.00"), new BigDecimal("5.00")))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getError())
                        .isEqualTo(ValidationError.EPISODE_COST_REQUIRED));
    }

    @Test
    void create_rejectsNonPositiveEpisodeCost() {
        assertThatThrownBy(() -> projectService.create("My project", BigDecimal.ZERO, new BigDecimal("50.00"), new BigDecimal("5.00")))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getError())
                        .isEqualTo(ValidationError.EPISODE_COST_NOT_POSITIVE));
    }

    @Test
    void update_trimsNameAndSavesChanges() {
        Project existing = TestEntities.project(1L, "Old name", new BigDecimal("100.00"));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(existing));

        Project updated = projectService.update(1L, "  New name  ", new BigDecimal("250.75"), new BigDecimal("100.00"), new BigDecimal("10.00"));

        assertThat(updated.getName()).isEqualTo("New name");
        assertThat(updated.getEpisodeCostRub()).isEqualByComparingTo("250.75");
    }

    @Test
    void update_rejectsBlankName() {
        assertThatThrownBy(() -> projectService.update(1L, "  ", new BigDecimal("100.00"), new BigDecimal("50.00"), new BigDecimal("5.00")))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getError())
                        .isEqualTo(ValidationError.PROJECT_NAME_REQUIRED));
    }

    @Test
    void update_rejectsMissingEpisodeCost() {
        assertThatThrownBy(() -> projectService.update(1L, "My project", null, new BigDecimal("50.00"), new BigDecimal("5.00")))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getError())
                        .isEqualTo(ValidationError.EPISODE_COST_REQUIRED));
    }

    @Test
    void update_rejectsNonPositiveEpisodeCost() {
        assertThatThrownBy(() -> projectService.update(1L, "My project", BigDecimal.ZERO, new BigDecimal("50.00"), new BigDecimal("5.00")))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getError())
                        .isEqualTo(ValidationError.EPISODE_COST_NOT_POSITIVE));
    }

    @Test
    void update_throwsWhenProjectNotFound() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.update(99L, "Name", new BigDecimal("100.00"), new BigDecimal("50.00"), new BigDecimal("5.00")))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Project not found: 99");
    }
}
