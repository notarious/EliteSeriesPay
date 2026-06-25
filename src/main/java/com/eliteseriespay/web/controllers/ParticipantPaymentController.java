package com.eliteseriespay.web.controllers;

import com.eliteseriespay.domain.Participant;
import com.eliteseriespay.domain.Payment;
import com.eliteseriespay.domain.PaymentSource;
import com.eliteseriespay.domain.PaymentStatus;
import com.eliteseriespay.domain.Project;
import com.eliteseriespay.domain.ProjectMembership;
import com.eliteseriespay.exception.NotFoundException;
import com.eliteseriespay.exception.ValidationException;
import com.eliteseriespay.service.ParticipantPaymentHistoryFilter;
import com.eliteseriespay.service.PaymentFormDefaults;
import com.eliteseriespay.service.ParticipantService;
import com.eliteseriespay.service.PaymentService;
import com.eliteseriespay.service.ProjectMembershipService;
import com.eliteseriespay.validation.ValidationError;
import com.eliteseriespay.web.form.PaymentForm;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
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
    public String history(@PathVariable Long participantId,
                          @RequestParam(value = "projectId", required = false) Long projectId,
                          @RequestParam(value = "source", required = false) PaymentSource source,
                          @RequestParam(value = "status", required = false) PaymentStatus status,
                          @RequestParam(value = "dateFrom", required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                          @RequestParam(value = "dateTo", required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
                          @RequestParam(value = "page", defaultValue = "0") int page,
                          @RequestParam(value = "size", defaultValue = "25") int size,
                          Model model) {
        ParticipantPaymentHistoryFilter filter = ParticipantPaymentHistoryFilter.of(
                projectId, source, status, dateFrom, dateTo, page, size);
        Participant participant = participantService.findById(participantId);
        Page<Payment> paymentsPage = paymentService.findParticipantPaymentHistory(participantId, filter);
        List<Project> filterProjects = paymentService.findProjectsInParticipantPaymentHistory(participantId);

        populateHistoryModel(model, participant, participantId, filter, paymentsPage, filterProjects);
        return "participants/payments";
    }

    @GetMapping("/new")
    public String createForm(@PathVariable Long participantId, Model model) {
        List<ProjectMembership> activeMemberships =
                projectMembershipService.findActiveByParticipantId(participantId);
        if (activeMemberships.isEmpty()) {
            return "redirect:/participants/" + participantId;
        }

        Set<Long> activeProjectIds = activeMemberships.stream()
                .map(membership -> membership.getProject().getId())
                .collect(Collectors.toSet());

        PaymentForm paymentForm = toPaymentForm(
                paymentService.getNewPaymentFormDefaults(participantId, activeProjectIds));
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

    @GetMapping("/{paymentId}/edit")
    public String editForm(@PathVariable Long participantId,
                           @PathVariable Long paymentId,
                           @RequestParam(value = "historyProjectId", required = false) Long historyProjectId,
                           @RequestParam(value = "historySource", required = false) PaymentSource historySource,
                           @RequestParam(value = "historyStatus", required = false) PaymentStatus historyStatus,
                           @RequestParam(value = "historyDateFrom", required = false)
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate historyDateFrom,
                           @RequestParam(value = "historyDateTo", required = false)
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate historyDateTo,
                           @RequestParam(value = "historyPage", defaultValue = "0") int historyPage,
                           @RequestParam(value = "historySize", defaultValue = "25") int historySize,
                           Model model) {
        List<ProjectMembership> activeMemberships =
                projectMembershipService.findActiveByParticipantId(participantId);
        if (activeMemberships.isEmpty()) {
            return historyRedirect(participantId, historyProjectId, historySource, historyStatus,
                    historyDateFrom, historyDateTo, historyPage, historySize);
        }

        Payment payment = paymentService.findById(participantId, paymentId);

        if (payment.getStatus() == PaymentStatus.VOIDED) {
            return historyRedirect(participantId, historyProjectId, historySource, historyStatus,
                    historyDateFrom, historyDateTo, historyPage, historySize);
        }

        PaymentForm paymentForm = toPaymentForm(payment);
        ParticipantPaymentHistoryFilter historyFilter = ParticipantPaymentHistoryFilter.of(
                historyProjectId, historySource, historyStatus,
                historyDateFrom, historyDateTo, historyPage, historySize);
        populateEditFormModel(model, participantId, paymentId, activeMemberships, paymentForm, historyFilter);
        return "participants/payment-edit";
    }

    @PostMapping("/{paymentId}/edit")
    public String update(@PathVariable Long participantId,
                         @PathVariable Long paymentId,
                         @Valid @ModelAttribute("paymentForm") PaymentForm paymentForm,
                         BindingResult bindingResult,
                         @RequestParam(value = "historyProjectId", required = false) Long historyProjectId,
                         @RequestParam(value = "historySource", required = false) PaymentSource historySource,
                         @RequestParam(value = "historyStatus", required = false) PaymentStatus historyStatus,
                         @RequestParam(value = "historyDateFrom", required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate historyDateFrom,
                         @RequestParam(value = "historyDateTo", required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate historyDateTo,
                         @RequestParam(value = "historyPage", defaultValue = "0") int historyPage,
                         @RequestParam(value = "historySize", defaultValue = "25") int historySize,
                         Model model) {
        List<ProjectMembership> activeMemberships =
                projectMembershipService.findActiveByParticipantId(participantId);
        ParticipantPaymentHistoryFilter historyFilter = ParticipantPaymentHistoryFilter.of(
                historyProjectId, historySource, historyStatus,
                historyDateFrom, historyDateTo, historyPage, historySize);

        if (bindingResult.hasErrors()) {
            populateEditFormModel(model, participantId, paymentId, activeMemberships, paymentForm, historyFilter);
            return "participants/payment-edit";
        }

        try {
            paymentService.update(
                    participantId,
                    paymentId,
                    paymentForm.getProjectId(),
                    paymentForm.getPaymentDate(),
                    paymentForm.getSource(),
                    paymentForm.getAmountOriginal(),
                    paymentForm.getCurrency(),
                    paymentForm.getExchangeRate(),
                    paymentForm.getComment());
        } catch (ValidationException ex) {
            rejectPaymentForm(bindingResult, ex);
            populateEditFormModel(model, participantId, paymentId, activeMemberships, paymentForm, historyFilter);
            return "participants/payment-edit";
        }

        return historyRedirect(participantId, historyFilter);
    }

    @PostMapping("/{paymentId}/void")
    public String voidPayment(@PathVariable Long participantId,
                              @PathVariable Long paymentId,
                              @RequestParam(value = "historyProjectId", required = false) Long historyProjectId,
                              @RequestParam(value = "historySource", required = false) PaymentSource historySource,
                              @RequestParam(value = "historyStatus", required = false) PaymentStatus historyStatus,
                              @RequestParam(value = "historyDateFrom", required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate historyDateFrom,
                              @RequestParam(value = "historyDateTo", required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate historyDateTo,
                              @RequestParam(value = "historyPage", defaultValue = "0") int historyPage,
                              @RequestParam(value = "historySize", defaultValue = "25") int historySize) {
        try {
            paymentService.voidPayment(participantId, paymentId);
        } catch (ValidationException ex) {
            if (ex.getError() != ValidationError.PAYMENT_ALREADY_VOIDED) {
                throw ex;
            }
        }

        return historyRedirect(participantId, historyProjectId, historySource, historyStatus,
                historyDateFrom, historyDateTo, historyPage, historySize);
    }

    @ExceptionHandler(NotFoundException.class)
    public String handleNotFound(@PathVariable Long participantId) {
        return "redirect:/participants/" + participantId + "/payments";
    }

    private void populateHistoryModel(Model model,
                                      Participant participant,
                                      Long participantId,
                                      ParticipantPaymentHistoryFilter filter,
                                      Page<Payment> paymentsPage,
                                      List<Project> filterProjects) {
        model.addAttribute("participant", participant);
        model.addAttribute("participantId", participantId);
        model.addAttribute("paymentsPage", paymentsPage);
        model.addAttribute("historyFilter", filter);
        model.addAttribute("filterProjects", filterProjects);
        model.addAttribute("hasAnyPayments", paymentService.hasAnyPayments(participantId));
        model.addAttribute("allowedPageSizes", ParticipantPaymentHistoryFilter.ALLOWED_PAGE_SIZES);
        model.addAttribute("activeMemberships", projectMembershipService.findActiveByParticipantId(participantId));
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

    private void populateEditFormModel(Model model,
                                       Long participantId,
                                       Long paymentId,
                                       List<ProjectMembership> activeMemberships,
                                       PaymentForm paymentForm,
                                       ParticipantPaymentHistoryFilter historyFilter) {
        Participant participant = participantService.findById(participantId);
        model.addAttribute("participant", participant);
        model.addAttribute("participantId", participantId);
        model.addAttribute("paymentId", paymentId);
        model.addAttribute("activeMemberships", activeMemberships);
        model.addAttribute("paymentForm", paymentForm);
        model.addAttribute("historyFilter", historyFilter);
    }

    private String historyRedirect(Long participantId, ParticipantPaymentHistoryFilter filter) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromPath("/participants/" + participantId + "/payments");
        appendHistoryQuery(builder, filter);
        return "redirect:" + builder.build().toUriString();
    }

    private String historyRedirect(Long participantId,
                                   Long projectId,
                                   PaymentSource source,
                                   PaymentStatus status,
                                   LocalDate dateFrom,
                                   LocalDate dateTo,
                                   int page,
                                   int size) {
        return historyRedirect(participantId, ParticipantPaymentHistoryFilter.of(
                projectId, source, status, dateFrom, dateTo, page, size));
    }

    private void appendHistoryQuery(UriComponentsBuilder builder, ParticipantPaymentHistoryFilter filter) {
        if (filter.projectId() != null) {
            builder.queryParam("projectId", filter.projectId());
        }
        if (filter.source() != null) {
            builder.queryParam("source", filter.source());
        }
        if (filter.status() != null) {
            builder.queryParam("status", filter.status());
        }
        if (filter.dateFrom() != null) {
            builder.queryParam("dateFrom", filter.dateFrom());
        }
        if (filter.dateTo() != null) {
            builder.queryParam("dateTo", filter.dateTo());
        }
        if (filter.page() > 0) {
            builder.queryParam("page", filter.page());
        }
        if (filter.pageSize() != ParticipantPaymentHistoryFilter.DEFAULT_PAGE_SIZE) {
            builder.queryParam("size", filter.pageSize());
        }
    }

    private PaymentForm toPaymentForm(Payment payment) {
        PaymentForm paymentForm = new PaymentForm();
        paymentForm.setProjectId(payment.getProject().getId());
        paymentForm.setPaymentDate(payment.getPaymentDate());
        paymentForm.setSource(payment.getSource());
        paymentForm.setAmountOriginal(payment.getAmountOriginal());
        paymentForm.setCurrency(payment.getCurrency());
        paymentForm.setExchangeRate(payment.getCurrency() == com.eliteseriespay.domain.PaymentCurrency.RUB
                ? null : payment.getExchangeRate());
        paymentForm.setComment(payment.getComment());
        return paymentForm;
    }

    private PaymentForm toPaymentForm(PaymentFormDefaults defaults) {
        PaymentForm paymentForm = new PaymentForm();
        paymentForm.setPaymentDate(defaults.paymentDate());
        paymentForm.setProjectId(defaults.projectId());
        paymentForm.setSource(defaults.source());
        paymentForm.setAmountOriginal(defaults.amountOriginal());
        paymentForm.setCurrency(defaults.currency());
        paymentForm.setExchangeRate(defaults.exchangeRate());
        return paymentForm;
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
            case PAYMENT_VOIDED, PAYMENT_ALREADY_VOIDED -> null;
            default -> throw new IllegalStateException("Unexpected validation error: " + error);
        };
        if (field != null) {
            bindingResult.rejectValue(field, error.name(), error.getMessage());
        } else {
            bindingResult.reject(error.name(), error.getMessage());
        }
    }
}
