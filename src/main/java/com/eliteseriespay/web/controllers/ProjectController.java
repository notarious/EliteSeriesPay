package com.eliteseriespay.web.controllers;

import com.eliteseriespay.domain.Project;
import com.eliteseriespay.exception.NotFoundException;
import com.eliteseriespay.exception.ValidationException;
import com.eliteseriespay.service.ProjectMembershipService;
import com.eliteseriespay.service.ProjectService;
import com.eliteseriespay.web.FormErrorMapper;
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
    private final ProjectMembershipService projectMembershipService;
    private final FormErrorMapper formErrorMapper;

    public ProjectController(ProjectService projectService,
                             ProjectMembershipService projectMembershipService,
                             FormErrorMapper formErrorMapper) {
        this.projectService = projectService;
        this.projectMembershipService = projectMembershipService;
        this.formErrorMapper = formErrorMapper;
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
            Project project = projectService.create(projectForm.getName(), projectForm.getEpisodeCostRub());
            return "redirect:/projects/" + project.getId();
        } catch (ValidationException ex) {
            formErrorMapper.rejectProjectForm(bindingResult, ex);
            return "projects/new";
        }
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        Project project = projectService.findById(id);
        model.addAttribute("project", project);
        model.addAttribute("memberships", projectMembershipService.findActiveByProjectId(id));
        return "projects/show";
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
        } catch (ValidationException ex) {
            formErrorMapper.rejectProjectForm(bindingResult, ex);
            model.addAttribute("projectId", id);
            return "projects/edit";
        }

        return "redirect:/projects/" + id;
    }

    @ExceptionHandler(NotFoundException.class)
    public String handleNotFound() {
        return "redirect:/projects";
    }
}
