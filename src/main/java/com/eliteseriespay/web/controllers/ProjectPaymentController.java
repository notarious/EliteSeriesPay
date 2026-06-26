package com.eliteseriespay.web.controllers;

import com.eliteseriespay.domain.BillingMode;
import com.eliteseriespay.domain.PaymentCurrency;
import com.eliteseriespay.domain.PaymentSource;
import com.eliteseriespay.domain.PaymentStatus;
import com.eliteseriespay.domain.Project;
import com.eliteseriespay.exception.NotFoundException;
import com.eliteseriespay.payment.history.PaymentHistoryRowView;
import com.eliteseriespay.payment.history.ProjectPaymentHistoryFilter;
import com.eliteseriespay.payment.history.ProjectPaymentHistoryService;
import com.eliteseriespay.service.ProjectService;
import com.eliteseriespay.web.ProjectPaymentHistoryNavigation;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/projects/{projectId}/payments")
public class ProjectPaymentController {

    private final ProjectService projectService;
    private final ProjectPaymentHistoryService projectPaymentHistoryService;

    public ProjectPaymentController(ProjectService projectService,
                                    ProjectPaymentHistoryService projectPaymentHistoryService) {
        this.projectService = projectService;
        this.projectPaymentHistoryService = projectPaymentHistoryService;
    }

    @GetMapping
    public String history(@PathVariable Long projectId,
                          @RequestParam(value = "participantId", required = false) Long participantId,
                          @RequestParam(value = "billingMode", required = false) BillingMode billingMode,
                          @RequestParam(value = "source", required = false) PaymentSource source,
                          @RequestParam(value = "currency", required = false) PaymentCurrency currency,
                          @RequestParam(value = "status", required = false) PaymentStatus status,
                          @RequestParam(value = "dateFrom", required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                          @RequestParam(value = "dateTo", required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
                          @RequestParam(value = "page", defaultValue = "0") int page,
                          @RequestParam(value = "size", defaultValue = "25") int size,
                          Model model) {
        Project project = projectService.findById(projectId);
        ProjectPaymentHistoryFilter filter = ProjectPaymentHistoryFilter.of(
                projectId, participantId, billingMode, source, currency, status, dateFrom, dateTo, page, size);
        Page<PaymentHistoryRowView> paymentsPage =
                projectPaymentHistoryService.findProjectPaymentHistory(projectId, filter);

        model.addAttribute("project", project);
        model.addAttribute("projectId", projectId);
        model.addAttribute("paymentsPage", paymentsPage);
        model.addAttribute("historyFilter", filter);
        model.addAttribute("filterParticipants",
                projectPaymentHistoryService.findParticipantsInProjectPaymentHistory(projectId));
        model.addAttribute("hasAnyPayments", projectPaymentHistoryService.hasAnyPayments(projectId));
        model.addAttribute("allowedPageSizes", ProjectPaymentHistoryFilter.ALLOWED_PAGE_SIZES);
        model.addAttribute("billingModes", BillingMode.values());
        model.addAttribute("paymentSources", PaymentSource.values());
        model.addAttribute("paymentCurrencies", PaymentCurrency.values());
        model.addAttribute("historyNavigation", ProjectPaymentHistoryNavigation.of(projectId, filter));
        return "projects/payments";
    }

    @ExceptionHandler(NotFoundException.class)
    public String handleNotFound(@PathVariable Long projectId) {
        return "redirect:/projects/" + projectId;
    }
}
