package com.eliteseriespay.web.controllers;

import com.eliteseriespay.domain.Participant;
import com.eliteseriespay.exception.NotFoundException;
import com.eliteseriespay.exception.ValidationException;
import com.eliteseriespay.service.ParticipantService;
import com.eliteseriespay.service.PaymentService;
import com.eliteseriespay.service.ProjectMembershipService;
import com.eliteseriespay.web.FormErrorMapper;
import com.eliteseriespay.web.form.AddParticipantToProjectForm;
import com.eliteseriespay.web.form.ParticipantEditForm;
import com.eliteseriespay.web.form.ParticipantForm;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
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
@RequestMapping("/participants")
public class ParticipantController {

    private final ParticipantService participantService;
    private final ProjectMembershipService projectMembershipService;
    private final PaymentService paymentService;
    private final FormErrorMapper formErrorMapper;

    public ParticipantController(ParticipantService participantService,
                                 ProjectMembershipService projectMembershipService,
                                 PaymentService paymentService,
                                 FormErrorMapper formErrorMapper) {
        this.participantService = participantService;
        this.projectMembershipService = projectMembershipService;
        this.paymentService = paymentService;
        this.formErrorMapper = formErrorMapper;
    }

    @GetMapping
    public String list(Model model) {
        List<Participant> participants = participantService.findAllOrderByName();
        Map<Long, Long> activeProjectCounts = projectMembershipService.countActiveProjectsByParticipantId();

        model.addAttribute("participants", participants);
        model.addAttribute("activeProjectCounts", activeProjectCounts);
        return "participants/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("participantForm", new ParticipantForm());
        return "participants/new";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("participantForm") ParticipantForm participantForm,
                         BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "participants/new";
        }

        try {
            Participant participant = participantService.create(
                    participantForm.getVkId(),
                    participantForm.getName(),
                    participantForm.getComment());
            return "redirect:/participants/" + participant.getId();
        } catch (ValidationException ex) {
            formErrorMapper.rejectParticipantForm(bindingResult, ex);
            return "participants/new";
        }
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        Participant participant = participantService.findById(id);

        model.addAttribute("participant", participant);
        model.addAttribute("activeMemberships", projectMembershipService.findActiveByParticipantId(id));
        model.addAttribute("leftMemberships", projectMembershipService.findLeftByParticipantId(id));
        model.addAttribute("paymentSummary", paymentService.getParticipantPaymentSummary(id));
        return "participants/show";
    }

    @GetMapping("/{id}/projects/new")
    public String addToProjectForm(@PathVariable Long id, Model model) {
        participantService.findById(id);

        model.addAttribute("participantId", id);
        model.addAttribute("availableProjects", projectMembershipService.findProjectsAvailableForParticipant(id));
        model.addAttribute("addParticipantToProjectForm", new AddParticipantToProjectForm());
        return "participants/add-to-project";
    }

    @PostMapping("/{id}/projects")
    public String addToProject(@PathVariable("id") Long participantId,
                               @Valid @ModelAttribute("addParticipantToProjectForm")
                               AddParticipantToProjectForm addParticipantToProjectForm,
                               BindingResult bindingResult,
                               Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("participantId", participantId);
            model.addAttribute("availableProjects",
                    projectMembershipService.findProjectsAvailableForParticipant(participantId));
            return "participants/add-to-project";
        }

        try {
            projectMembershipService.addParticipantToProject(
                    addParticipantToProjectForm.getProjectId(), participantId);
        } catch (ValidationException ex) {
            formErrorMapper.rejectAddToProjectForm(bindingResult, ex);
            model.addAttribute("participantId", participantId);
            model.addAttribute("availableProjects",
                    projectMembershipService.findProjectsAvailableForParticipant(participantId));
            return "participants/add-to-project";
        }

        return "redirect:/participants/" + participantId;
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Participant participant = participantService.findById(id);

        ParticipantEditForm participantEditForm = new ParticipantEditForm();
        participantEditForm.setVkId(participant.getVkId());
        participantEditForm.setName(participant.getName());
        participantEditForm.setComment(participant.getComment());

        model.addAttribute("participantId", id);
        model.addAttribute("participantEditForm", participantEditForm);
        return "participants/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("participantEditForm") ParticipantEditForm participantEditForm,
                         BindingResult bindingResult,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("participantId", id);
            return "participants/edit";
        }

        try {
            participantService.update(
                    id,
                    participantEditForm.getVkId(),
                    participantEditForm.getName(),
                    participantEditForm.getComment());
        } catch (ValidationException ex) {
            formErrorMapper.rejectParticipantForm(bindingResult, ex);
            model.addAttribute("participantId", id);
            return "participants/edit";
        }

        return "redirect:/participants/" + id;
    }

    @ExceptionHandler(NotFoundException.class)
    public String handleNotFound() {
        return "redirect:/participants";
    }
}
