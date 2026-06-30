package com.eliteseriespay.web.controllers;

import com.eliteseriespay.domain.Participant;
import com.eliteseriespay.domain.BillingMode;
import com.eliteseriespay.billing.MembershipPaymentStatusFilter;
import com.eliteseriespay.exception.NotFoundException;
import com.eliteseriespay.exception.ValidationException;
import com.eliteseriespay.membership.MembershipAddResult;
import com.eliteseriespay.service.ProjectMembershipService;
import com.eliteseriespay.validation.ValidationError;
import com.eliteseriespay.web.FormErrorMapper;
import com.eliteseriespay.web.ProjectParticipantsFilter;
import com.eliteseriespay.web.ProjectParticipantsNavigation;
import com.eliteseriespay.web.form.ExistingParticipantForm;
import com.eliteseriespay.web.form.ParticipantEditForm;
import com.eliteseriespay.web.form.ProjectNewParticipantForm;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequestMapping("/projects/{projectId}/participants")
public class ProjectParticipantController {

    private final ProjectMembershipService projectMembershipService;
    private final FormErrorMapper formErrorMapper;

    public ProjectParticipantController(ProjectMembershipService projectMembershipService,
                                        FormErrorMapper formErrorMapper) {
        this.projectMembershipService = projectMembershipService;
        this.formErrorMapper = formErrorMapper;
    }

    @GetMapping("/new")
    public String createForm(@PathVariable Long projectId, Model model) {
        model.addAttribute("projectId", projectId);
        populateNewFormModel(model, projectId, null);
        model.addAttribute("billingModes", BillingMode.values());
        return "projects/participants/new";
    }

    @PostMapping("/existing")
    public String addExisting(@PathVariable Long projectId,
                              @Valid @ModelAttribute("existingParticipantForm")
                              ExistingParticipantForm existingParticipantForm,
                              BindingResult bindingResult,
                              Model model) {
        if (bindingResult.hasErrors()) {
            populateNewFormModel(model, projectId, "existing");
            model.addAttribute("billingModes", BillingMode.values());
            return "projects/participants/new";
        }

        try {
            MembershipAddResult result = projectMembershipService.addParticipantToProject(
                    projectId,
                    existingParticipantForm.getParticipantId(),
                    existingParticipantForm.getBillingMode());
            return resolveAddResultRedirect(projectId, result);
        } catch (ValidationException ex) {
            formErrorMapper.rejectExistingParticipantForm(bindingResult, ex);
            populateNewFormModel(model, projectId, "existing");
            model.addAttribute("billingModes", BillingMode.values());
            return "projects/participants/new";
        }
    }

    @PostMapping
    public String create(@PathVariable Long projectId,
                         @Valid @ModelAttribute("projectNewParticipantForm")
                         ProjectNewParticipantForm projectNewParticipantForm,
                         BindingResult bindingResult,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("projectId", projectId);
            populateNewFormModel(model, projectId, "new");
            model.addAttribute("billingModes", BillingMode.values());
            return "projects/participants/new";
        }

        try {
            MembershipAddResult result = projectMembershipService.addToProject(
                    projectId,
                    projectNewParticipantForm.getVkId(),
                    projectNewParticipantForm.getName(),
                    projectNewParticipantForm.getComment(),
                    projectNewParticipantForm.getBillingMode());
            return resolveAddResultRedirect(projectId, result);
        } catch (ValidationException ex) {
            formErrorMapper.rejectProjectParticipantForm(bindingResult, ex);
            model.addAttribute("projectId", projectId);
            populateNewFormModel(model, projectId, "new");
            model.addAttribute("billingModes", BillingMode.values());
            return "projects/participants/new";
        }
    }

    @GetMapping("/{participantId}/edit")
    public String editForm(@PathVariable Long projectId,
                           @PathVariable Long participantId,
                           Model model) {
        Participant participant = projectMembershipService.findActiveParticipant(projectId, participantId);

        ParticipantEditForm participantEditForm = new ParticipantEditForm();
        participantEditForm.setVkId(participant.getVkId());
        participantEditForm.setName(participant.getName());
        participantEditForm.setComment(participant.getComment());

        model.addAttribute("projectId", projectId);
        model.addAttribute("participantId", participantId);
        model.addAttribute("participantEditForm", participantEditForm);
        return "projects/participants/edit";
    }

    @PostMapping("/{participantId}/edit")
    public String update(@PathVariable Long projectId,
                         @PathVariable Long participantId,
                         @Valid @ModelAttribute("participantEditForm") ParticipantEditForm participantEditForm,
                         BindingResult bindingResult,
                         Model model) {
        if (bindingResult.hasErrors()) {
            populateEditModel(model, projectId, participantId);
            return "projects/participants/edit";
        }

        try {
            projectMembershipService.updateParticipant(
                    projectId,
                    participantId,
                    participantEditForm.getVkId(),
                    participantEditForm.getName(),
                    participantEditForm.getComment());
        } catch (ValidationException ex) {
            formErrorMapper.rejectProjectParticipantForm(bindingResult, ex);
            populateEditModel(model, projectId, participantId);
            return "projects/participants/edit";
        }

        return "redirect:/projects/" + projectId;
    }

    @PostMapping("/{participantId}/remove")
    public String remove(@PathVariable Long projectId,
                         @PathVariable Long participantId,
                         @RequestParam(required = false) BillingMode billingMode,
                         @RequestParam(required = false) MembershipPaymentStatusFilter paymentStatus) {
        try {
            projectMembershipService.removeFromProject(projectId, participantId);
        } catch (ValidationException ex) {
            if (ex.getError() != ValidationError.NOT_AN_ACTIVE_MEMBER) {
                throw ex;
            }
        }

        return "redirect:" + ProjectParticipantsNavigation.listUrl(
                projectId, ProjectParticipantsNavigation.filterFromParams(billingMode, paymentStatus));
    }

    @ExceptionHandler(NotFoundException.class)
    public String handleNotFound(@PathVariable Long projectId) {
        return "redirect:/projects/" + projectId;
    }

    @ExceptionHandler(ValidationException.class)
    public String handleUncaughtValidation(ValidationException ex, @PathVariable Long projectId) {
        if (ex.getError() == ValidationError.NOT_AN_ACTIVE_MEMBER) {
            return "redirect:/projects/" + projectId;
        }
        throw ex;
    }

    private String resolveAddResultRedirect(Long projectId, MembershipAddResult result) {
        if (result.requiresInitialPayment()) {
            String returnTo = ProjectParticipantsNavigation.listUrl(projectId, ProjectParticipantsFilter.empty());
            String redirectUrl = UriComponentsBuilder
                    .fromPath("/participants/" + result.participantId() + "/payments/new")
                    .queryParam("initialMembership", true)
                    .queryParam("projectId", projectId)
                    .queryParam("returnTo", returnTo)
                    .build()
                    .toUriString();
            return "redirect:" + redirectUrl;
        }
        return "redirect:/projects/" + projectId;
    }

    private void populateEditModel(Model model, Long projectId, Long participantId) {
        projectMembershipService.findActiveParticipant(projectId, participantId);
        model.addAttribute("projectId", projectId);
        model.addAttribute("participantId", participantId);
    }

    private void populateNewFormModel(Model model, Long projectId, String addMode) {
        var availableParticipants = projectMembershipService.findParticipantsAvailableForProject(projectId);
        model.addAttribute("availableParticipants", availableParticipants);
        model.addAttribute("billingModes", BillingMode.values());
        if (!model.containsAttribute("projectNewParticipantForm")) {
            ProjectNewParticipantForm projectNewParticipantForm = new ProjectNewParticipantForm();
            projectNewParticipantForm.setBillingMode(BillingMode.SUBSCRIPTION);
            model.addAttribute("projectNewParticipantForm", projectNewParticipantForm);
        }
        if (!model.containsAttribute("existingParticipantForm")) {
            ExistingParticipantForm existingParticipantForm = new ExistingParticipantForm();
            existingParticipantForm.setBillingMode(BillingMode.SUBSCRIPTION);
            model.addAttribute("existingParticipantForm", existingParticipantForm);
        }
        if (!model.containsAttribute("addMode")) {
            String resolvedMode = addMode != null
                    ? addMode
                    : (availableParticipants.isEmpty() ? "new" : "existing");
            model.addAttribute("addMode", resolvedMode);
        }
    }
}
