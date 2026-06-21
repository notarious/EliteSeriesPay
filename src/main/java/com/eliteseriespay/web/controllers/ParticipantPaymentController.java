package com.eliteseriespay.web.controllers;

import com.eliteseriespay.domain.Participant;
import com.eliteseriespay.domain.ProjectMembership;
import com.eliteseriespay.exception.NotFoundException;
import com.eliteseriespay.exception.ValidationException;
import com.eliteseriespay.service.ParticipantService;
import com.eliteseriespay.service.PaymentService;
import com.eliteseriespay.service.ProjectMembershipService;
import com.eliteseriespay.validation.ValidationError;
import com.eliteseriespay.web.form.PaymentForm;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
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
@RequestMapping("/participants/{participantId}/payments")
public class ParticipantPaymentController {

    private final ParticipantService participantService;
    private final ProjectMembershipService projectMembershipService;
    private final PaymentService paymentService;

    public ParticipantPaymentController(ParticipantService participantService,
                                        ProjectMembershipService projectMembershipService,
                                        PaymentService paymentService) {
        this.participantService = participantService;
        this.projectMembershipService = projectMembershipService;
        this.paymentService = paymentService;
    }

    @GetMapping
    public String history(@PathVariable Long participantId, Model model) {
        Participant participant = participantService.findById(participantId);
        model.addAttribute("participant", participant);
        model.addAttribute("participantId", participantId);
        model.addAttribute("payments", paymentService.findByParticipantId(participantId));
        model.addAttribute("activeMemberships", projectMembershipService.findActiveByParticipantId(participantId));
        return "participants/payments";
    }

    @GetMapping("/new")
    public String createForm(@PathVariable Long participantId, Model model) {
        List<ProjectMembership> activeMemberships =
                projectMembershipService.findActiveByParticipantId(participantId);
        if (activeMemberships.isEmpty()) {
            return "redirect:/participants/" + participantId;
        }

        PaymentForm paymentForm = new PaymentForm();
        paymentForm.setPaymentDate(LocalDate.now());
        populateFormModel(model, participantId, activeMemberships, paymentForm);
        return "participants/payment-new";
    }

    @PostMapping
    public String create(@PathVariable Long participantId,
                         @Valid @ModelAttribute("paymentForm") PaymentForm paymentForm,
                         BindingResult bindingResult,
                         Model model) {
        List<ProjectMembership> activeMemberships =
                projectMembershipService.findActiveByParticipantId(participantId);
        if (activeMemberships.isEmpty()) {
            return "redirect:/participants/" + participantId;
        }

        if (bindingResult.hasErrors()) {
            populateFormModel(model, participantId, activeMemberships, paymentForm);
            return "participants/payment-new";
        }

        try {
            paymentService.create(
                    participantId,
                    paymentForm.getProjectId(),
                    paymentForm.getPaymentDate(),
                    paymentForm.getSource(),
                    paymentForm.getAmountOriginal(),
                    paymentForm.getCurrency(),
                    paymentForm.getExchangeRate(),
                    paymentForm.getComment());
        } catch (ValidationException ex) {
            rejectPaymentForm(bindingResult, ex);
            populateFormModel(model, participantId, activeMemberships, paymentForm);
            return "participants/payment-new";
        }

        return "redirect:/participants/" + participantId;
    }

    @ExceptionHandler(NotFoundException.class)
    public String handleNotFound() {
        return "redirect:/participants";
    }

    private void populateFormModel(Model model,
                                   Long participantId,
                                   List<ProjectMembership> activeMemberships,
                                   PaymentForm paymentForm) {
        Participant participant = participantService.findById(participantId);
        model.addAttribute("participant", participant);
        model.addAttribute("participantId", participantId);
        model.addAttribute("activeMemberships", activeMemberships);
        model.addAttribute("paymentForm", paymentForm);
    }

    private void rejectPaymentForm(BindingResult bindingResult, ValidationException ex) {
        ValidationError error = ex.getError();
        String field = switch (error) {
            case NOT_AN_ACTIVE_MEMBER -> "projectId";
            case PAYMENT_DATE_REQUIRED -> "paymentDate";
            case PAYMENT_AMOUNT_REQUIRED, PAYMENT_AMOUNT_NOT_POSITIVE -> "amountOriginal";
            case EXCHANGE_RATE_REQUIRED, EXCHANGE_RATE_NOT_POSITIVE -> "exchangeRate";
            case PAYMENT_SOURCE_REQUIRED -> "source";
            case PAYMENT_CURRENCY_REQUIRED -> "currency";
            default -> throw new IllegalStateException("Unexpected validation error: " + error);
        };
        bindingResult.rejectValue(field, error.name(), error.getMessage());
    }
}
