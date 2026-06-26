package com.eliteseriespay.web.controllers;

import com.eliteseriespay.domain.Participant;
import com.eliteseriespay.domain.Payment;
import com.eliteseriespay.domain.BillingMode;
import com.eliteseriespay.domain.PaymentCurrency;
import com.eliteseriespay.domain.PaymentSource;
import com.eliteseriespay.domain.PaymentStatus;
import com.eliteseriespay.domain.Project;
import com.eliteseriespay.domain.ProjectMembership;
import com.eliteseriespay.exception.NotFoundException;
import com.eliteseriespay.exception.ValidationException;
import com.eliteseriespay.payment.history.ParticipantPaymentHistoryFilter;
import com.eliteseriespay.payment.PaymentFormDefaults;
import com.eliteseriespay.service.ParticipantService;
import com.eliteseriespay.service.PaymentService;
import com.eliteseriespay.service.ProjectMembershipService;
import com.eliteseriespay.payment.history.ProjectPaymentHistoryFilter;
import com.eliteseriespay.service.ProjectService;
import com.eliteseriespay.validation.ValidationError;
import com.eliteseriespay.web.ParticipantPaymentHistoryNavigation;
import com.eliteseriespay.web.ProjectPaymentHistoryNavigation;
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
    private final ProjectService projectService;
    private final PaymentService paymentService;

    public ParticipantPaymentController(ParticipantService participantService,
                                        ProjectMembershipService projectMembershipService,
                                        ProjectService projectService,
                                        PaymentService paymentService) {
        this.participantService = participantService;
        this.projectMembershipService = projectMembershipService;
        this.projectService = projectService;
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
    public String createForm(@PathVariable Long participantId,
                             @RequestParam(value = "initialMembership", defaultValue = "false") boolean initialMembership,
                             @RequestParam(value = "projectId", required = false) Long projectId,
                             @RequestParam(value = "returnTo", required = false) String returnTo,
                             Model model) {
        if (initialMembership) {
            if (projectId == null) {
                return "redirect:/participants/" + participantId;
            }
            projectService.findById(projectId);
            participantService.findById(participantId);

            PaymentForm paymentForm = new PaymentForm();
            paymentForm.setProjectId(projectId);
            paymentForm.setPaymentDate(LocalDate.now());
            populateInitialMembershipFormModel(model, participantId, projectId, returnTo, paymentForm);
            return "participants/payment-new";
        }

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
        if (projectId != null && activeProjectIds.contains(projectId)) {
            paymentForm.setProjectId(projectId);
        }
        populateFormModel(model, participantId, activeMemberships, paymentForm, false, null);
        return "participants/payment-new";
    }

    @PostMapping
    public String create(@PathVariable Long participantId,
                         @Valid @ModelAttribute("paymentForm") PaymentForm paymentForm,
                         BindingResult bindingResult,
                         @RequestParam(value = "initialMembership", defaultValue = "false") boolean initialMembership,
                         @RequestParam(value = "returnTo", required = false) String returnTo,
                         Model model) {
        if (initialMembership) {
            return createInitialMembershipPayment(participantId, paymentForm, bindingResult, returnTo, model);
        }

        List<ProjectMembership> activeMemberships =
                projectMembershipService.findActiveByParticipantId(participantId);
        if (activeMemberships.isEmpty()) {
            return "redirect:/participants/" + participantId;
        }

        if (bindingResult.hasErrors()) {
            populateFormModel(model, participantId, activeMemberships, paymentForm, false, null);
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
            populateFormModel(model, participantId, activeMemberships, paymentForm, false, null);
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
                           @RequestParam(value = "projectHistoryProjectId", required = false) Long projectHistoryProjectId,
                           @RequestParam(value = "projectHistoryParticipantId", required = false) Long projectHistoryParticipantId,
                           @RequestParam(value = "projectHistoryBillingMode", required = false) BillingMode projectHistoryBillingMode,
                           @RequestParam(value = "projectHistorySource", required = false) PaymentSource projectHistorySource,
                           @RequestParam(value = "projectHistoryCurrency", required = false) PaymentCurrency projectHistoryCurrency,
                           @RequestParam(value = "projectHistoryStatus", required = false) PaymentStatus projectHistoryStatus,
                           @RequestParam(value = "projectHistoryDateFrom", required = false)
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate projectHistoryDateFrom,
                           @RequestParam(value = "projectHistoryDateTo", required = false)
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate projectHistoryDateTo,
                           @RequestParam(value = "projectHistoryPage", defaultValue = "0") int projectHistoryPage,
                           @RequestParam(value = "projectHistorySize", defaultValue = "25") int projectHistorySize,
                           Model model) {
        ProjectPaymentHistoryFilter projectHistoryFilter = resolveProjectHistoryFilter(
                projectHistoryProjectId,
                projectHistoryParticipantId,
                projectHistoryBillingMode,
                projectHistorySource,
                projectHistoryCurrency,
                projectHistoryStatus,
                projectHistoryDateFrom,
                projectHistoryDateTo,
                projectHistoryPage,
                projectHistorySize);

        List<ProjectMembership> activeMemberships =
                projectMembershipService.findActiveByParticipantId(participantId);
        if (activeMemberships.isEmpty()) {
            return paymentHistoryRedirect(participantId, historyProjectId, historySource, historyStatus,
                    historyDateFrom, historyDateTo, historyPage, historySize, projectHistoryFilter);
        }

        Payment payment = paymentService.findById(participantId, paymentId);

        if (payment.getStatus() == PaymentStatus.VOIDED) {
            return paymentHistoryRedirect(participantId, historyProjectId, historySource, historyStatus,
                    historyDateFrom, historyDateTo, historyPage, historySize, projectHistoryFilter);
        }

        PaymentForm paymentForm = toPaymentForm(payment);
        ParticipantPaymentHistoryFilter historyFilter = ParticipantPaymentHistoryFilter.of(
                historyProjectId, historySource, historyStatus,
                historyDateFrom, historyDateTo, historyPage, historySize);
        populateEditFormModel(model, participantId, paymentId, activeMemberships, paymentForm,
                historyFilter, projectHistoryFilter);
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
                         @RequestParam(value = "projectHistoryProjectId", required = false) Long projectHistoryProjectId,
                         @RequestParam(value = "projectHistoryParticipantId", required = false) Long projectHistoryParticipantId,
                         @RequestParam(value = "projectHistoryBillingMode", required = false) BillingMode projectHistoryBillingMode,
                         @RequestParam(value = "projectHistorySource", required = false) PaymentSource projectHistorySource,
                         @RequestParam(value = "projectHistoryCurrency", required = false) PaymentCurrency projectHistoryCurrency,
                         @RequestParam(value = "projectHistoryStatus", required = false) PaymentStatus projectHistoryStatus,
                         @RequestParam(value = "projectHistoryDateFrom", required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate projectHistoryDateFrom,
                         @RequestParam(value = "projectHistoryDateTo", required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate projectHistoryDateTo,
                         @RequestParam(value = "projectHistoryPage", defaultValue = "0") int projectHistoryPage,
                         @RequestParam(value = "projectHistorySize", defaultValue = "25") int projectHistorySize,
                         Model model) {
        List<ProjectMembership> activeMemberships =
                projectMembershipService.findActiveByParticipantId(participantId);
        ParticipantPaymentHistoryFilter historyFilter = ParticipantPaymentHistoryFilter.of(
                historyProjectId, historySource, historyStatus,
                historyDateFrom, historyDateTo, historyPage, historySize);
        ProjectPaymentHistoryFilter projectHistoryFilter = resolveProjectHistoryFilter(
                projectHistoryProjectId,
                projectHistoryParticipantId,
                projectHistoryBillingMode,
                projectHistorySource,
                projectHistoryCurrency,
                projectHistoryStatus,
                projectHistoryDateFrom,
                projectHistoryDateTo,
                projectHistoryPage,
                projectHistorySize);

        if (bindingResult.hasErrors()) {
            populateEditFormModel(model, participantId, paymentId, activeMemberships, paymentForm,
                    historyFilter, projectHistoryFilter);
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
            populateEditFormModel(model, participantId, paymentId, activeMemberships, paymentForm,
                    historyFilter, projectHistoryFilter);
            return "participants/payment-edit";
        }

        return paymentHistoryRedirect(participantId, historyFilter, projectHistoryFilter);
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
                              @RequestParam(value = "historySize", defaultValue = "25") int historySize,
                              @RequestParam(value = "projectHistoryProjectId", required = false) Long projectHistoryProjectId,
                              @RequestParam(value = "projectHistoryParticipantId", required = false) Long projectHistoryParticipantId,
                              @RequestParam(value = "projectHistoryBillingMode", required = false) BillingMode projectHistoryBillingMode,
                              @RequestParam(value = "projectHistorySource", required = false) PaymentSource projectHistorySource,
                              @RequestParam(value = "projectHistoryCurrency", required = false) PaymentCurrency projectHistoryCurrency,
                              @RequestParam(value = "projectHistoryStatus", required = false) PaymentStatus projectHistoryStatus,
                              @RequestParam(value = "projectHistoryDateFrom", required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate projectHistoryDateFrom,
                              @RequestParam(value = "projectHistoryDateTo", required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate projectHistoryDateTo,
                              @RequestParam(value = "projectHistoryPage", defaultValue = "0") int projectHistoryPage,
                              @RequestParam(value = "projectHistorySize", defaultValue = "25") int projectHistorySize) {
        try {
            paymentService.voidPayment(participantId, paymentId);
        } catch (ValidationException ex) {
            if (ex.getError() != ValidationError.PAYMENT_ALREADY_VOIDED) {
                throw ex;
            }
        }

        ProjectPaymentHistoryFilter projectHistoryFilter = resolveProjectHistoryFilter(
                projectHistoryProjectId,
                projectHistoryParticipantId,
                projectHistoryBillingMode,
                projectHistorySource,
                projectHistoryCurrency,
                projectHistoryStatus,
                projectHistoryDateFrom,
                projectHistoryDateTo,
                projectHistoryPage,
                projectHistorySize);

        return paymentHistoryRedirect(participantId, historyProjectId, historySource, historyStatus,
                historyDateFrom, historyDateTo, historyPage, historySize, projectHistoryFilter);
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
        model.addAttribute("historyNavigation", ParticipantPaymentHistoryNavigation.of(participantId, filter));
    }

    private void populateFormModel(Model model,
                                   Long participantId,
                                   List<ProjectMembership> activeMemberships,
                                   PaymentForm paymentForm,
                                   boolean initialMembership,
                                   String returnTo) {
        Participant participant = participantService.findById(participantId);
        model.addAttribute("participant", participant);
        model.addAttribute("participantId", participantId);
        model.addAttribute("activeMemberships", activeMemberships);
        model.addAttribute("paymentForm", paymentForm);
        model.addAttribute("initialMembership", initialMembership);
        model.addAttribute("returnTo", returnTo);
    }

    private void populateInitialMembershipFormModel(Model model,
                                                    Long participantId,
                                                    Long projectId,
                                                    String returnTo,
                                                    PaymentForm paymentForm) {
        Participant participant = participantService.findById(participantId);
        Project project = projectService.findById(projectId);
        model.addAttribute("participant", participant);
        model.addAttribute("participantId", participantId);
        model.addAttribute("initialMembershipProject", project);
        model.addAttribute("paymentForm", paymentForm);
        model.addAttribute("initialMembership", true);
        model.addAttribute("returnTo", returnTo != null ? returnTo : "/participants/" + participantId);
    }

    private String createInitialMembershipPayment(Long participantId,
                                                PaymentForm paymentForm,
                                                BindingResult bindingResult,
                                                String returnTo,
                                                Model model) {
        Long projectId = paymentForm.getProjectId();
        if (projectId == null) {
            return "redirect:/participants/" + participantId;
        }

        if (bindingResult.hasErrors()) {
            populateInitialMembershipFormModel(model, participantId, projectId, returnTo, paymentForm);
            return "participants/payment-new";
        }

        try {
            paymentService.createInitialMembershipPayment(
                    participantId,
                    projectId,
                    paymentForm.getPaymentDate(),
                    paymentForm.getSource(),
                    paymentForm.getAmountOriginal(),
                    paymentForm.getCurrency(),
                    paymentForm.getExchangeRate(),
                    paymentForm.getComment());
        } catch (ValidationException ex) {
            rejectPaymentForm(bindingResult, ex);
            populateInitialMembershipFormModel(model, participantId, projectId, returnTo, paymentForm);
            return "participants/payment-new";
        }

        String redirectTarget = returnTo != null ? returnTo : "/participants/" + participantId;
        return "redirect:" + redirectTarget;
    }

    private void populateEditFormModel(Model model,
                                       Long participantId,
                                       Long paymentId,
                                       List<ProjectMembership> activeMemberships,
                                       PaymentForm paymentForm,
                                       ParticipantPaymentHistoryFilter historyFilter,
                                       ProjectPaymentHistoryFilter projectHistoryFilter) {
        Participant participant = participantService.findById(participantId);
        model.addAttribute("participant", participant);
        model.addAttribute("participantId", participantId);
        model.addAttribute("paymentId", paymentId);
        model.addAttribute("activeMemberships", activeMemberships);
        model.addAttribute("paymentForm", paymentForm);
        model.addAttribute("historyFilter", historyFilter);
        model.addAttribute("historyNavigation", ParticipantPaymentHistoryNavigation.of(participantId, historyFilter));
        if (projectHistoryFilter != null) {
            model.addAttribute("projectHistoryNavigation",
                    ProjectPaymentHistoryNavigation.of(projectHistoryFilter.projectId(), projectHistoryFilter));
        }
    }

    private ProjectPaymentHistoryFilter resolveProjectHistoryFilter(Long projectHistoryProjectId,
                                                                    Long projectHistoryParticipantId,
                                                                    BillingMode projectHistoryBillingMode,
                                                                    PaymentSource projectHistorySource,
                                                                    PaymentCurrency projectHistoryCurrency,
                                                                    PaymentStatus projectHistoryStatus,
                                                                    LocalDate projectHistoryDateFrom,
                                                                    LocalDate projectHistoryDateTo,
                                                                    int projectHistoryPage,
                                                                    int projectHistorySize) {
        if (projectHistoryProjectId == null) {
            return null;
        }
        return ProjectPaymentHistoryNavigation.filterFromEditParams(
                projectHistoryProjectId,
                projectHistoryParticipantId,
                projectHistoryBillingMode,
                projectHistorySource,
                projectHistoryCurrency,
                projectHistoryStatus,
                projectHistoryDateFrom,
                projectHistoryDateTo,
                projectHistoryPage,
                projectHistorySize);
    }

    private String paymentHistoryRedirect(Long participantId,
                                          ParticipantPaymentHistoryFilter historyFilter,
                                          ProjectPaymentHistoryFilter projectHistoryFilter) {
        if (projectHistoryFilter != null) {
            return projectHistoryRedirect(projectHistoryFilter);
        }
        return participantHistoryRedirect(participantId, historyFilter);
    }

    private String paymentHistoryRedirect(Long participantId,
                                          Long historyProjectId,
                                          PaymentSource historySource,
                                          PaymentStatus historyStatus,
                                          LocalDate historyDateFrom,
                                          LocalDate historyDateTo,
                                          int historyPage,
                                          int historySize,
                                          ProjectPaymentHistoryFilter projectHistoryFilter) {
        if (projectHistoryFilter != null) {
            return projectHistoryRedirect(projectHistoryFilter);
        }
        return participantHistoryRedirect(participantId, ParticipantPaymentHistoryFilter.of(
                historyProjectId, historySource, historyStatus,
                historyDateFrom, historyDateTo, historyPage, historySize));
    }

    private String projectHistoryRedirect(ProjectPaymentHistoryFilter filter) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromPath("/projects/" + filter.projectId() + "/payments");
        ProjectPaymentHistoryNavigation.appendListQuery(builder, filter);
        return "redirect:" + builder.build().toUriString();
    }

    private String participantHistoryRedirect(Long participantId, ParticipantPaymentHistoryFilter filter) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromPath("/participants/" + participantId + "/payments");
        ParticipantPaymentHistoryNavigation.appendListQuery(builder, filter);
        return "redirect:" + builder.build().toUriString();
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
            case PAYMENT_AMOUNT_REQUIRED, PAYMENT_AMOUNT_NOT_POSITIVE,
                 INITIAL_SUBSCRIPTION_PAYMENT_INSUFFICIENT -> "amountOriginal";
            case EXCHANGE_RATE_REQUIRED, EXCHANGE_RATE_NOT_POSITIVE -> "exchangeRate";
            case PAYMENT_SOURCE_REQUIRED -> "source";
            case PAYMENT_CURRENCY_REQUIRED, INITIAL_SUBSCRIPTION_PAYMENT_USD_NOT_SUPPORTED -> "currency";
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
