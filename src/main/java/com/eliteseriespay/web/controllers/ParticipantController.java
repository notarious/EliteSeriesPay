package com.eliteseriespay.web.controllers;

import com.eliteseriespay.domain.BillingMode;
import com.eliteseriespay.domain.Participant;
import com.eliteseriespay.exception.NotFoundException;
import com.eliteseriespay.exception.ValidationException;
import com.eliteseriespay.membership.MembershipAddResult;
import com.eliteseriespay.billing.MembershipBillingService;
import com.eliteseriespay.service.ParticipantService;
import com.eliteseriespay.service.PaymentService;
import com.eliteseriespay.service.ProjectMembershipService;
import com.eliteseriespay.validation.ValidationError;
import com.eliteseriespay.web.FormErrorMapper;
import com.eliteseriespay.web.form.AddParticipantToProjectForm;
import com.eliteseriespay.web.form.ParticipantEditForm;
import com.eliteseriespay.web.form.ParticipantForm;
import jakarta.validation.Valid;
import java.time.YearMonth;
import java.util.Map;
import org.springframework.data.domain.Page;
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
@RequestMapping("/participants")
public class ParticipantController {

    private final ParticipantService participantService;
    private final ProjectMembershipService projectMembershipService;
    private final PaymentService paymentService;
    private final MembershipBillingService membershipBillingService;
    private final FormErrorMapper formErrorMapper;

    public ParticipantController(ParticipantService participantService,
                                 ProjectMembershipService projectMembershipService,
                                 PaymentService paymentService,
                                 MembershipBillingService membershipBillingService,
                                 FormErrorMapper formErrorMapper) {
        this.participantService = participantService;
        this.projectMembershipService = projectMembershipService;
        this.paymentService = paymentService;
        this.membershipBillingService = membershipBillingService;
        this.formErrorMapper = formErrorMapper;
    }

    @GetMapping
    public String list(@RequestParam(value = "q", required = false) String searchQuery,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       @RequestParam(value = "size", defaultValue = "50") int size,
                       Model model) {
        Page<Participant> participantsPage = participantService.findParticipants(searchQuery, page, size);
        Map<Long, Long> activeProjectCounts = projectMembershipService.countActiveProjectsByParticipantIds(
                participantsPage.getContent().stream().map(Participant::getId).toList());

        model.addAttribute("participantsPage", participantsPage);
        model.addAttribute("activeProjectCounts", activeProjectCounts);
        model.addAttribute("searchQuery", searchQuery == null ? "" : searchQuery);
        model.addAttribute("pageSize", ParticipantService.ALLOWED_PAGE_SIZES.contains(size)
                ? size
                : ParticipantService.DEFAULT_PAGE_SIZE);
        model.addAttribute("allowedPageSizes", ParticipantService.ALLOWED_PAGE_SIZES);
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
        var activeMemberships = projectMembershipService.findActiveByParticipantId(id);
        var membershipBillingViews = membershipBillingService.buildParticipantMembershipViews(
                activeMemberships, YearMonth.now());

        model.addAttribute("participant", participant);
        model.addAttribute("activeMemberships", activeMemberships);
        model.addAttribute("membershipBillingViews", membershipBillingViews);
        model.addAttribute("leftMemberships", projectMembershipService.findLeftByParticipantId(id));
        model.addAttribute("paymentSummary", paymentService.getParticipantPaymentSummary(id));
        return "participants/show";
    }

    @GetMapping("/{id}/projects/new")
    public String addToProjectForm(@PathVariable Long id, Model model) {
        participantService.findById(id);

        model.addAttribute("participantId", id);
        model.addAttribute("availableProjects", projectMembershipService.findProjectsAvailableForParticipant(id));
        AddParticipantToProjectForm form = new AddParticipantToProjectForm();
        form.setBillingMode(BillingMode.SUBSCRIPTION);
        model.addAttribute("addParticipantToProjectForm", form);
        model.addAttribute("billingModes", BillingMode.values());
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
            model.addAttribute("billingModes", BillingMode.values());
            return "participants/add-to-project";
        }

        try {
            MembershipAddResult result = projectMembershipService.addParticipantToProject(
                    addParticipantToProjectForm.getProjectId(),
                    participantId,
                    addParticipantToProjectForm.getBillingMode());
            return resolveAddResultRedirect(addParticipantToProjectForm.getProjectId(), participantId, result);
        } catch (ValidationException ex) {
            formErrorMapper.rejectAddToProjectForm(bindingResult, ex);
            model.addAttribute("participantId", participantId);
            model.addAttribute("availableProjects",
                    projectMembershipService.findProjectsAvailableForParticipant(participantId));
            model.addAttribute("billingModes", BillingMode.values());
            return "participants/add-to-project";
        }
    }

    private String resolveAddResultRedirect(Long projectId, Long participantId, MembershipAddResult result) {
        if (result.requiresInitialPayment()) {
            String returnTo = "/participants/" + participantId;
            String redirectUrl = UriComponentsBuilder
                    .fromPath("/participants/" + participantId + "/payments/new")
                    .queryParam("initialMembership", true)
                    .queryParam("projectId", projectId)
                    .queryParam("returnTo", returnTo)
                    .build()
                    .toUriString();
            return "redirect:" + redirectUrl;
        }
        return "redirect:/participants/" + participantId;
    }

    @PostMapping("/{participantId}/projects/{projectId}/remove")
    public String removeFromProject(@PathVariable Long participantId, @PathVariable Long projectId) {
        try {
            projectMembershipService.removeFromProject(projectId, participantId);
        } catch (ValidationException ex) {
            if (ex.getError() != ValidationError.NOT_AN_ACTIVE_MEMBER) {
                throw ex;
            }
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
