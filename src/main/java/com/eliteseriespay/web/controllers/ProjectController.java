package com.eliteseriespay.web.controllers;

import com.eliteseriespay.domain.BillingMode;
import com.eliteseriespay.domain.Participant;
import com.eliteseriespay.domain.Project;
import com.eliteseriespay.exception.NotFoundException;
import com.eliteseriespay.exception.ValidationException;
import com.eliteseriespay.billing.BillingModeFilter;
import com.eliteseriespay.billing.MembershipBillingService;
import com.eliteseriespay.billing.MembershipPaymentStatusFilter;
import com.eliteseriespay.report.ProjectReportService;
import com.eliteseriespay.service.ProjectMembershipService;
import com.eliteseriespay.service.ProjectService;
import com.eliteseriespay.web.FormErrorMapper;
import com.eliteseriespay.web.form.ProjectForm;
import jakarta.validation.Valid;
import java.time.YearMonth;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectMembershipService projectMembershipService;
    private final MembershipBillingService membershipBillingService;
    private final ProjectReportService projectReportService;
    private final FormErrorMapper formErrorMapper;

    public ProjectController(ProjectService projectService,
                             ProjectMembershipService projectMembershipService,
                             MembershipBillingService membershipBillingService,
                             ProjectReportService projectReportService,
                             FormErrorMapper formErrorMapper) {
        this.projectService = projectService;
        this.projectMembershipService = projectMembershipService;
        this.membershipBillingService = membershipBillingService;
        this.projectReportService = projectReportService;
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
            Project project = projectService.create(
                    projectForm.getName(),
                    projectForm.getEpisodeCostRub(),
                    projectForm.getMonthlyFeeRub(),
                    projectForm.getMonthlyFeeEur());
            return "redirect:/projects/" + project.getId();
        } catch (ValidationException ex) {
            formErrorMapper.rejectProjectForm(bindingResult, ex);
            return "projects/new";
        }
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id,
                       @RequestParam(required = false) BillingMode billingMode,
                       @RequestParam(required = false) MembershipPaymentStatusFilter paymentStatus,
                       Model model) {
        Project project = projectService.findById(id);
        var memberships = projectMembershipService.findActiveByProjectId(id);
        var participants = membershipBillingService.buildProjectParticipantViews(
                memberships,
                BillingModeFilter.of(billingMode),
                paymentStatus != null ? paymentStatus : MembershipPaymentStatusFilter.ALL,
                YearMonth.now());

        model.addAttribute("project", project);
        model.addAttribute("participants", participants);
        model.addAttribute("selectedBillingMode", billingMode);
        model.addAttribute("selectedPaymentStatus", paymentStatus);
        model.addAttribute("billingModes", BillingMode.values());
        model.addAttribute("paymentStatusFilters", MembershipPaymentStatusFilter.values());
        model.addAttribute("monthlySummary", projectReportService.buildCurrentMonthSummary(id));
        return "projects/show";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Project project = projectService.findById(id);

        ProjectForm projectForm = new ProjectForm();
        projectForm.setName(project.getName());
        projectForm.setEpisodeCostRub(project.getEpisodeCostRub());
        projectForm.setMonthlyFeeRub(project.getMonthlyFeeRub());
        projectForm.setMonthlyFeeEur(project.getMonthlyFeeEur());

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
            projectService.update(
                    id,
                    projectForm.getName(),
                    projectForm.getEpisodeCostRub(),
                    projectForm.getMonthlyFeeRub(),
                    projectForm.getMonthlyFeeEur());
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
