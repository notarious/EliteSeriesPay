package com.eliteseriespay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eliteseriespay.domain.MembershipStatus;
import com.eliteseriespay.domain.Participant;
import com.eliteseriespay.domain.Project;
import com.eliteseriespay.domain.ProjectMembership;
import com.eliteseriespay.exception.NotFoundException;
import com.eliteseriespay.exception.ValidationException;
import com.eliteseriespay.repository.ParticipantRepository;
import com.eliteseriespay.repository.ProjectMembershipRepository;
import com.eliteseriespay.repository.ProjectRepository;
import com.eliteseriespay.support.TestEntities;
import com.eliteseriespay.validation.ValidationError;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectMembershipServiceTest {

    private static final long PROJECT_ID = 1L;
    private static final long PARTICIPANT_ID = 10L;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private ProjectMembershipRepository projectMembershipRepository;

    private ProjectMembershipService projectMembershipService;

    @BeforeEach
    void setUp() {
        ProjectService projectService = new ProjectService(projectRepository);
        ParticipantService participantService = new ParticipantService(participantRepository);
        projectMembershipService = new ProjectMembershipService(
                projectService, participantService, projectMembershipRepository);
    }

    @Test
    void addToProject_createsParticipantAndMembership() {
        Project project = TestEntities.project(PROJECT_ID, "Series", new BigDecimal("1000.00"));
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(participantRepository.findByVkId("12345")).thenReturn(Optional.empty());
        when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> {
            Participant saved = invocation.getArgument(0);
            TestEntities.setId(saved, PARTICIPANT_ID);
            return saved;
        });
        when(projectMembershipRepository.findByProject_IdAndParticipant_Id(PROJECT_ID, PARTICIPANT_ID))
                .thenReturn(Optional.empty());
        when(projectMembershipRepository.save(any(ProjectMembership.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectMembership membership = projectMembershipService.addToProject(
                PROJECT_ID, "12345", "Ivan", null);

        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(membership.getProject()).isEqualTo(project);
        assertThat(membership.getParticipant().getId()).isEqualTo(PARTICIPANT_ID);
        assertThat(membership.getParticipant().getVkId()).isEqualTo("12345");
        assertThat(membership.getParticipant().getName()).isEqualTo("Ivan");
        verify(projectMembershipRepository).save(any(ProjectMembership.class));
    }

    @Test
    void addToProject_reusesExistingParticipant() {
        Project project = TestEntities.project(PROJECT_ID, "Series", new BigDecimal("1000.00"));
        Participant participant = TestEntities.participant(PARTICIPANT_ID, "12345", "Ivan", null);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(participantRepository.findByVkId("12345")).thenReturn(Optional.of(participant));
        when(projectMembershipRepository.findByProject_IdAndParticipant_Id(PROJECT_ID, PARTICIPANT_ID))
                .thenReturn(Optional.empty());
        when(projectMembershipRepository.save(any(ProjectMembership.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectMembership membership = projectMembershipService.addToProject(
                PROJECT_ID, "12345", "Ivan", null);

        assertThat(membership.getParticipant()).isEqualTo(participant);
        verify(participantRepository, never()).save(any(Participant.class));
        verify(projectMembershipRepository).save(any(ProjectMembership.class));
    }

    @Test
    void addToProject_reactivatesLeftMembership() {
        Project project = TestEntities.project(PROJECT_ID, "Series", new BigDecimal("1000.00"));
        Participant participant = TestEntities.participant(PARTICIPANT_ID, "12345", "Ivan", null);
        ProjectMembership existing = new ProjectMembership(project, participant, MembershipStatus.LEFT);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(participantRepository.findByVkId("12345")).thenReturn(Optional.of(participant));
        when(projectMembershipRepository.findByProject_IdAndParticipant_Id(PROJECT_ID, PARTICIPANT_ID))
                .thenReturn(Optional.of(existing));

        ProjectMembership membership = projectMembershipService.addToProject(
                PROJECT_ID, "12345", "Ivan", null);

        assertThat(membership).isSameAs(existing);
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        verify(projectMembershipRepository, never()).save(any(ProjectMembership.class));
    }

    @Test
    void addToProject_rejectsAlreadyActiveMember() {
        Project project = TestEntities.project(PROJECT_ID, "Series", new BigDecimal("1000.00"));
        Participant participant = TestEntities.participant(PARTICIPANT_ID, "12345", "Ivan", null);
        ProjectMembership existing = new ProjectMembership(project, participant, MembershipStatus.ACTIVE);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(participantRepository.findByVkId("12345")).thenReturn(Optional.of(participant));
        when(projectMembershipRepository.findByProject_IdAndParticipant_Id(PROJECT_ID, PARTICIPANT_ID))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> projectMembershipService.addToProject(PROJECT_ID, "12345", "Ivan", null))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getError())
                        .isEqualTo(ValidationError.PARTICIPANT_ALREADY_ACTIVE));
    }

    @Test
    void addToProject_rejectsBlankVkId() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(
                TestEntities.project(PROJECT_ID, "Series", new BigDecimal("1000.00"))));

        assertThatThrownBy(() -> projectMembershipService.addToProject(PROJECT_ID, "  ", "Ivan", null))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getError())
                        .isEqualTo(ValidationError.VK_ID_REQUIRED));
    }

    @Test
    void addToProject_rejectsBlankName() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(
                TestEntities.project(PROJECT_ID, "Series", new BigDecimal("1000.00"))));

        assertThatThrownBy(() -> projectMembershipService.addToProject(PROJECT_ID, "12345", "  ", null))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getError())
                        .isEqualTo(ValidationError.PARTICIPANT_NAME_REQUIRED));
    }

    @Test
    void addToProject_throwsWhenProjectNotFound() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectMembershipService.addToProject(99L, "12345", "Ivan", null))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Project not found: 99");
    }

    @Test
    void removeFromProject_marksMembershipAsLeft() {
        Project project = TestEntities.project(PROJECT_ID, "Series", new BigDecimal("1000.00"));
        Participant participant = TestEntities.participant(PARTICIPANT_ID, "12345", "Ivan", null);
        ProjectMembership membership = new ProjectMembership(project, participant, MembershipStatus.ACTIVE);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(participantRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant));
        when(projectMembershipRepository.findByProject_IdAndParticipant_Id(PROJECT_ID, PARTICIPANT_ID))
                .thenReturn(Optional.of(membership));

        projectMembershipService.removeFromProject(PROJECT_ID, PARTICIPANT_ID);

        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.LEFT);
        verify(projectMembershipRepository, never()).save(any(ProjectMembership.class));
    }

    @Test
    void removeFromProject_rejectsWhenNotActiveMember() {
        Project project = TestEntities.project(PROJECT_ID, "Series", new BigDecimal("1000.00"));
        Participant participant = TestEntities.participant(PARTICIPANT_ID, "12345", "Ivan", null);
        ProjectMembership membership = new ProjectMembership(project, participant, MembershipStatus.LEFT);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(participantRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant));
        when(projectMembershipRepository.findByProject_IdAndParticipant_Id(PROJECT_ID, PARTICIPANT_ID))
                .thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> projectMembershipService.removeFromProject(PROJECT_ID, PARTICIPANT_ID))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getError())
                        .isEqualTo(ValidationError.NOT_AN_ACTIVE_MEMBER));
    }

    @Test
    void removeFromProject_throwsWhenParticipantNotFound() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(
                TestEntities.project(PROJECT_ID, "Series", new BigDecimal("1000.00"))));
        when(participantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectMembershipService.removeFromProject(PROJECT_ID, 99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Participant not found: 99");
    }

    @Test
    void findActiveParticipant_returnsParticipantForActiveMembership() {
        Project project = TestEntities.project(PROJECT_ID, "Series", new BigDecimal("1000.00"));
        Participant participant = TestEntities.participant(PARTICIPANT_ID, "12345", "Ivan", null);
        ProjectMembership membership = new ProjectMembership(project, participant, MembershipStatus.ACTIVE);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(participantRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant));
        when(projectMembershipRepository.findByProject_IdAndParticipant_Id(PROJECT_ID, PARTICIPANT_ID))
                .thenReturn(Optional.of(membership));

        Participant result = projectMembershipService.findActiveParticipant(PROJECT_ID, PARTICIPANT_ID);

        assertThat(result).isEqualTo(participant);
    }

    @Test
    void findActiveParticipant_rejectsLeftMembership() {
        Project project = TestEntities.project(PROJECT_ID, "Series", new BigDecimal("1000.00"));
        Participant participant = TestEntities.participant(PARTICIPANT_ID, "12345", "Ivan", null);
        ProjectMembership membership = new ProjectMembership(project, participant, MembershipStatus.LEFT);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(participantRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant));
        when(projectMembershipRepository.findByProject_IdAndParticipant_Id(PROJECT_ID, PARTICIPANT_ID))
                .thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> projectMembershipService.findActiveParticipant(PROJECT_ID, PARTICIPANT_ID))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getError())
                        .isEqualTo(ValidationError.NOT_AN_ACTIVE_MEMBER));
    }

    @Test
    void findActiveByProjectId_throwsWhenProjectNotFound() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectMembershipService.findActiveByProjectId(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Project not found: 99");
    }

    @Test
    void findActiveByProjectId_returnsActiveMemberships() {
        Project project = TestEntities.project(PROJECT_ID, "Series", new BigDecimal("1000.00"));
        Participant participant = TestEntities.participant(PARTICIPANT_ID, "12345", "Ivan", null);
        ProjectMembership activeMembership = new ProjectMembership(project, participant, MembershipStatus.ACTIVE);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(projectMembershipRepository.findByProjectIdAndStatus(PROJECT_ID, MembershipStatus.ACTIVE))
                .thenReturn(List.of(activeMembership));

        List<ProjectMembership> memberships = projectMembershipService.findActiveByProjectId(PROJECT_ID);

        assertThat(memberships).containsExactly(activeMembership);
    }

    @Test
    void updateParticipant_requiresActiveMembership() {
        Project project = TestEntities.project(PROJECT_ID, "Series", new BigDecimal("1000.00"));
        Participant participant = TestEntities.participant(PARTICIPANT_ID, "12345", "Ivan", null);
        ProjectMembership membership = new ProjectMembership(project, participant, MembershipStatus.ACTIVE);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(participantRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant));
        when(projectMembershipRepository.findByProject_IdAndParticipant_Id(PROJECT_ID, PARTICIPANT_ID))
                .thenReturn(Optional.of(membership));

        Participant result = projectMembershipService.updateParticipant(
                PROJECT_ID, PARTICIPANT_ID, "Petr", "note");

        assertThat(result.getName()).isEqualTo("Petr");
        assertThat(result.getComment()).isEqualTo("note");
    }
}
