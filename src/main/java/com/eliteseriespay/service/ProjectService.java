package com.eliteseriespay.service;

import com.eliteseriespay.domain.MembershipStatus;
import com.eliteseriespay.domain.Project;
import com.eliteseriespay.exception.NotFoundException;
import com.eliteseriespay.repository.ProjectRepository;
import com.eliteseriespay.util.Texts;
import com.eliteseriespay.validation.ProjectValidator;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Transactional(readOnly = true)
    public List<Project> findAll() {
        return projectRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Project> findAvailableForParticipant(Long participantId) {
        return projectRepository.findAvailableForParticipant(participantId, MembershipStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Project findById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Project", id));
    }

    @Transactional
    public Project create(String name,
                          BigDecimal episodeCostRub,
                          BigDecimal monthlyFeeRub,
                          BigDecimal monthlyFeeEur) {
        String normalizedName = Texts.trim(name);
        validateProject(normalizedName, episodeCostRub, monthlyFeeRub, monthlyFeeEur);

        Project project = new Project(normalizedName, episodeCostRub, monthlyFeeRub, monthlyFeeEur);
        return projectRepository.save(project);
    }

    @Transactional
    public Project update(Long id,
                          String name,
                          BigDecimal episodeCostRub,
                          BigDecimal monthlyFeeRub,
                          BigDecimal monthlyFeeEur) {
        String normalizedName = Texts.trim(name);
        validateProject(normalizedName, episodeCostRub, monthlyFeeRub, monthlyFeeEur);

        Project project = findById(id);
        project.updateDetails(normalizedName, episodeCostRub, monthlyFeeRub, monthlyFeeEur);
        return project;
    }

    private void validateProject(String name,
                                 BigDecimal episodeCostRub,
                                 BigDecimal monthlyFeeRub,
                                 BigDecimal monthlyFeeEur) {
        ProjectValidator.validateName(name);
        ProjectValidator.validateEpisodeCost(episodeCostRub);
        ProjectValidator.validateMonthlyFeeRub(monthlyFeeRub);
        ProjectValidator.validateMonthlyFeeEur(monthlyFeeEur);
    }
}
