package com.eliteseriespay.web.controllers;

import com.eliteseriespay.domain.Participant;
import com.eliteseriespay.exception.NotFoundException;
import com.eliteseriespay.exception.ValidationException;
import com.eliteseriespay.service.ProjectMembershipService;
import com.eliteseriespay.validation.ValidationError;
import com.eliteseriespay.web.FormErrorMapper;
import com.eliteseriespay.web.form.ExistingParticipantForm;
import com.eliteseriespay.web.form.ParticipantEditForm;
import com.eliteseriespay.web.form.ParticipantForm;
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
            return "projects/participants/new";
        }

        try {
            projectMembershipService.addParticipantToProject(
                    projectId, existingParticipantForm.getParticipantId());
        } catch (ValidationException ex) {
            formErrorMapper.rejectExistingParticipantForm(bindingResult, ex);
            populateNewFormModel(model, projectId, "existing");
            return "projects/participants/new";
        }

        return "redirect:/projects/" + projectId;
    }

    @PostMapping
    public String create(@PathVariable Long projectId,
                         @Valid @ModelAttribute("participantForm") ParticipantForm participantForm,
                         BindingResult bindingResult,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("projectId", projectId);
            populateNewFormModel(model, projectId, "new");
            return "projects/participants/new";
        }

        try {
            projectMembershipService.addToProject(
                    projectId,
                    participantForm.getVkId(),
                    participantForm.getName(),
                    participantForm.getComment());
        } catch (ValidationException ex) {
            formErrorMapper.rejectProjectParticipantForm(bindingResult, ex);
            model.addAttribute("projectId", projectId);
            populateNewFormModel(model, projectId, "new");
            return "projects/participants/new";
        }

        return "redirect:/projects/" + projectId;
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
    public String remove(@PathVariable Long projectId, @PathVariable Long participantId) {
        try {
            projectMembershipService.removeFromProject(projectId, participantId);
        } catch (ValidationException ex) {
            if (ex.getError() != ValidationError.NOT_AN_ACTIVE_MEMBER) {
                throw ex;
            }
        }

        return "redirect:/projects/" + projectId;
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

    private void populateEditModel(Model model, Long projectId, Long participantId) {
        projectMembershipService.findActiveParticipant(projectId, participantId);
        model.addAttribute("projectId", projectId);
        model.addAttribute("participantId", participantId);
    }

    private void populateNewFormModel(Model model, Long projectId, String addMode) {
        var availableParticipants = projectMembershipService.findParticipantsAvailableForProject(projectId);
        model.addAttribute("availableParticipants", availableParticipants);
        if (!model.containsAttribute("existingParticipantForm")) {
            model.addAttribute("existingParticipantForm", new ExistingParticipantForm());
        }
        if (!model.containsAttribute("participantForm")) {
            model.addAttribute("participantForm", new ParticipantForm());
        }
        if (!model.containsAttribute("addMode")) {
            String resolvedMode = addMode != null
                    ? addMode
                    : (availableParticipants.isEmpty() ? "new" : "existing");
            model.addAttribute("addMode", resolvedMode);
        }
    }
}
