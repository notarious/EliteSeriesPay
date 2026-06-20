package com.eliteseriespay.service;

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
    public Project findById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Project", id));
    }

    @Transactional
    public Project create(String name, BigDecimal episodeCostRub) {
        String normalizedName = Texts.trim(name);
        validateProject(normalizedName, episodeCostRub);

        Project project = new Project(normalizedName, episodeCostRub);
        return projectRepository.save(project);
    }

    @Transactional
    public Project update(Long id, String name, BigDecimal episodeCostRub) {
        String normalizedName = Texts.trim(name);
        validateProject(normalizedName, episodeCostRub);

        Project project = findById(id);
        project.updateDetails(normalizedName, episodeCostRub);
        return project;
    }

    private void validateProject(String name, BigDecimal episodeCostRub) {
        ProjectValidator.validateName(name);
        ProjectValidator.validateEpisodeCost(episodeCostRub);
    }
}
