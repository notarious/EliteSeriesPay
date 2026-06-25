package com.eliteseriespay.service;

import com.eliteseriespay.domain.MembershipStatus;
import com.eliteseriespay.domain.Participant;
import com.eliteseriespay.domain.Project;
import com.eliteseriespay.domain.ProjectMembership;
import com.eliteseriespay.exception.ValidationException;
import com.eliteseriespay.repository.ProjectMembershipRepository;
import com.eliteseriespay.validation.ValidationError;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Collection;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectMembershipService {

    private final ProjectService projectService;
    private final ParticipantService participantService;
    private final ProjectMembershipRepository projectMembershipRepository;

    public ProjectMembershipService(ProjectService projectService,
                                    ParticipantService participantService,
                                    ProjectMembershipRepository projectMembershipRepository) {
        this.projectService = projectService;
        this.participantService = participantService;
        this.projectMembershipRepository = projectMembershipRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjectMembership> findActiveByProjectId(Long projectId) {
        projectService.findById(projectId);
        return projectMembershipRepository.findByProjectIdAndStatus(projectId, MembershipStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<ProjectMembership> findActiveByParticipantId(Long participantId) {
        participantService.findById(participantId);
        return projectMembershipRepository.findByParticipantIdAndStatus(participantId, MembershipStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<ProjectMembership> findLeftByParticipantId(Long participantId) {
        participantService.findById(participantId);
        return projectMembershipRepository.findByParticipantIdAndStatus(participantId, MembershipStatus.LEFT);
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> countActiveProjectsByParticipantId() {
        return projectMembershipRepository.countGroupedByParticipantId(MembershipStatus.ACTIVE).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> countActiveProjectsByParticipantIds(Collection<Long> participantIds) {
        if (participantIds.isEmpty()) {
            return Map.of();
        }
        return projectMembershipRepository
                .countGroupedByParticipantIdIn(MembershipStatus.ACTIVE, participantIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }

    @Transactional
    public ProjectMembership addToProject(Long projectId, String vkId, String name, String comment) {
        Project project = projectService.findById(projectId);
        Participant participant = participantService.findOrCreate(vkId, name, comment);
        return activateOrCreateMembership(project, participant);
    }

    @Transactional
    public ProjectMembership addParticipantToProject(Long projectId, Long participantId) {
        Project project = projectService.findById(projectId);
        Participant participant = participantService.findById(participantId);
        return activateOrCreateMembership(project, participant);
    }

    @Transactional(readOnly = true)
    public List<Participant> findParticipantsAvailableForProject(Long projectId) {
        projectService.findById(projectId);
        return participantService.findAvailableForProject(projectId);
    }

    @Transactional(readOnly = true)
    public List<Project> findProjectsAvailableForParticipant(Long participantId) {
        participantService.findById(participantId);
        return projectService.findAvailableForParticipant(participantId);
    }

    @Transactional(readOnly = true)
    public ProjectMembership getActiveMembership(Long projectId, Long participantId) {
        projectService.findById(projectId);
        participantService.findById(participantId);

        ProjectMembership membership = projectMembershipRepository
                .findByProject_IdAndParticipant_Id(projectId, participantId)
                .orElseThrow(() -> new ValidationException(ValidationError.NOT_AN_ACTIVE_MEMBER));

        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new ValidationException(ValidationError.NOT_AN_ACTIVE_MEMBER);
        }

        return membership;
    }

    @Transactional(readOnly = true)
    public Participant findActiveParticipant(Long projectId, Long participantId) {
        return getActiveMembership(projectId, participantId).getParticipant();
    }

    @Transactional
    public Participant updateParticipant(Long projectId, Long participantId, String vkId, String name, String comment) {
        getActiveMembership(projectId, participantId);
        return participantService.update(participantId, vkId, name, comment);
    }

    @Transactional
    public void removeFromProject(Long projectId, Long participantId) {
        ProjectMembership membership = getActiveMembership(projectId, participantId);
        membership.markLeft();
    }

    private ProjectMembership activateOrCreateMembership(Project project, Participant participant) {
        Optional<ProjectMembership> existingMembership = projectMembershipRepository
                .findByProject_IdAndParticipant_Id(project.getId(), participant.getId());

        if (existingMembership.isPresent()) {
            ProjectMembership membership = existingMembership.get();
            if (membership.getStatus() == MembershipStatus.ACTIVE) {
                throw new ValidationException(ValidationError.PARTICIPANT_ALREADY_ACTIVE);
            }
            membership.markActive();
            return membership;
        }

        ProjectMembership membership = new ProjectMembership(project, participant, MembershipStatus.ACTIVE);
        return projectMembershipRepository.save(membership);
    }
}
