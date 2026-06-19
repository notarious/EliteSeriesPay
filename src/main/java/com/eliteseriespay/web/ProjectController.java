package com.eliteseriespay.web;

import com.eliteseriespay.domain.Project;
import com.eliteseriespay.service.ProjectNotFoundException;
import com.eliteseriespay.service.ProjectService;
import com.eliteseriespay.service.ProjectValidationError;
import com.eliteseriespay.service.ProjectValidationException;
import com.eliteseriespay.web.form.ProjectForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("projects", projectService.findAll());
        return "projects/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("projectForm", new ProjectForm());
        return "projects/new";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("projectForm") ProjectForm projectForm,
                         BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "projects/new";
        }

        try {
            projectService.create(projectForm.getName(), projectForm.getEpisodeCostRub());
        } catch (ProjectValidationException ex) {
            rejectValidationError(bindingResult, ex);
            return "projects/new";
        }

        return "redirect:/projects";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Project project = projectService.findById(id);

        ProjectForm projectForm = new ProjectForm();
        projectForm.setName(project.getName());
        projectForm.setEpisodeCostRub(project.getEpisodeCostRub());

        model.addAttribute("projectForm", projectForm);
        model.addAttribute("projectId", id);
        return "projects/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("projectForm") ProjectForm projectForm,
                         BindingResult bindingResult,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("projectId", id);
            return "projects/edit";
        }

        try {
            projectService.update(id, projectForm.getName(), projectForm.getEpisodeCostRub());
        } catch (ProjectValidationException ex) {
            rejectValidationError(bindingResult, ex);
            model.addAttribute("projectId", id);
            return "projects/edit";
        }

        return "redirect:/projects";
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    public String handleProjectNotFound() {
        return "redirect:/projects";
    }

    private void rejectValidationError(BindingResult bindingResult, ProjectValidationException ex) {
        ProjectValidationError error = ex.getError();
        String field = switch (error) {
            case NAME_REQUIRED -> "name";
            case EPISODE_COST_REQUIRED, EPISODE_COST_NOT_POSITIVE -> "episodeCostRub";
        };
        bindingResult.rejectValue(field, error.name(), error.getMessage());
    }
}
