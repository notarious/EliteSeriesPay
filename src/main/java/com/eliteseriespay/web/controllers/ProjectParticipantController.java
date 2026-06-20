package com.eliteseriespay.web.controllers;

import com.eliteseriespay.domain.Participant;
import com.eliteseriespay.exception.NotFoundException;
import com.eliteseriespay.exception.ValidationException;
import com.eliteseriespay.service.ProjectMembershipService;
import com.eliteseriespay.validation.ValidationError;
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

    public ProjectParticipantController(ProjectMembershipService projectMembershipService) {
        this.projectMembershipService = projectMembershipService;
    }

    @GetMapping("/new")
    public String createForm(@PathVariable Long projectId, Model model) {
        model.addAttribute("projectId", projectId);
        model.addAttribute("participantForm", new ParticipantForm());
        return "projects/participants/new";
    }

    @PostMapping
    public String create(@PathVariable Long projectId,
                         @Valid @ModelAttribute("participantForm") ParticipantForm participantForm,
                         BindingResult bindingResult,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("projectId", projectId);
            return "projects/participants/new";
        }

        try {
            projectMembershipService.addToProject(
                    projectId,
                    participantForm.getVkId(),
                    participantForm.getName(),
                    participantForm.getComment());
        } catch (ValidationException ex) {
            rejectValidationError(bindingResult, ex, true);
            model.addAttribute("projectId", projectId);
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
            rejectValidationError(bindingResult, ex, true);
            populateEditModel(model, projectId, participantId);
            return "projects/participants/edit";
        }

        return "redirect:/projects/" + projectId;
    }

    @PostMapping("/{participantId}/remove")
    public String remove(@PathVariable Long projectId, @PathVariable Long participantId) {
        try {
            projectMembershipService.removeFromProject(projectId, participantId);
        } catch (ValidationException ignored) {
            // Already removed or never a member — redirect silently.
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
        Participant participant = projectMembershipService.findActiveParticipant(projectId, participantId);
        model.addAttribute("projectId", projectId);
        model.addAttribute("participantId", participantId);
    }

    private void rejectValidationError(BindingResult bindingResult,
                                         ValidationException ex,
                                         boolean includeVkIdErrors) {
        ValidationError error = ex.getError();
        switch (error) {
            case VK_ID_REQUIRED, VK_ID_ALREADY_EXISTS -> {
                if (includeVkIdErrors) {
                    bindingResult.rejectValue("vkId", error.name(), error.getMessage());
                }
            }
            case PARTICIPANT_NAME_REQUIRED -> bindingResult.rejectValue("name", error.name(), error.getMessage());
            case PARTICIPANT_ALREADY_ACTIVE -> {
                if (includeVkIdErrors) {
                    bindingResult.rejectValue("vkId", error.name(), error.getMessage());
                }
            }
            case NOT_AN_ACTIVE_MEMBER -> bindingResult.rejectValue("name", error.name(), error.getMessage());
            default -> throw new IllegalStateException("Unexpected validation error: " + error);
        }
    }
}
