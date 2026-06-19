package com.eliteseriespay.service;

import com.eliteseriespay.domain.Project;
import com.eliteseriespay.repository.ProjectRepository;
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
                .orElseThrow(() -> new ProjectNotFoundException(id));
    }

    @Transactional
    public Project create(String name, BigDecimal episodeCostRub) {
        String normalizedName = normalizeName(name);
        validate(normalizedName, episodeCostRub);

        Project project = new Project(normalizedName, episodeCostRub);
        return projectRepository.save(project);
    }

    @Transactional
    public Project update(Long id, String name, BigDecimal episodeCostRub) {
        String normalizedName = normalizeName(name);
        validate(normalizedName, episodeCostRub);

        Project project = findById(id);
        project.updateDetails(normalizedName, episodeCostRub);
        return projectRepository.save(project);
    }

    private String normalizeName(String name) {
        return name == null ? null : name.trim();
    }

    private void validate(String name, BigDecimal episodeCostRub) {
        if (name == null || name.isBlank()) {
            throw new ProjectValidationException(ProjectValidationError.NAME_REQUIRED);
        }
        if (episodeCostRub == null) {
            throw new ProjectValidationException(ProjectValidationError.EPISODE_COST_REQUIRED);
        }
        if (episodeCostRub.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ProjectValidationException(ProjectValidationError.EPISODE_COST_NOT_POSITIVE);
        }
    }
}
