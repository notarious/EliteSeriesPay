package com.eliteseriespay.web.controllers;

import com.eliteseriespay.domain.Project;
import com.eliteseriespay.exception.NotFoundException;
import com.eliteseriespay.report.ProjectMonthlyReport;
import com.eliteseriespay.report.ProjectReportService;
import com.eliteseriespay.service.ProjectService;
import java.time.YearMonth;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/projects/{projectId}/reports")
public class ProjectReportController {

    private final ProjectService projectService;
    private final ProjectReportService projectReportService;

    public ProjectReportController(ProjectService projectService,
                                 ProjectReportService projectReportService) {
        this.projectService = projectService;
        this.projectReportService = projectReportService;
    }

    @GetMapping
    public String reports(@PathVariable Long projectId,
                          @RequestParam(required = false) Integer month,
                          @RequestParam(required = false) Integer year,
                          Model model) {
        Project project = projectService.findById(projectId);
        YearMonth selectedMonth = resolveMonth(month, year);
        ProjectMonthlyReport report = projectReportService.buildMonthlyReport(projectId, selectedMonth);

        model.addAttribute("project", project);
        model.addAttribute("projectId", projectId);
        model.addAttribute("report", report);
        model.addAttribute("selectedMonth", selectedMonth.getMonthValue());
        model.addAttribute("selectedYear", selectedMonth.getYear());
        return "projects/reports";
    }

    private YearMonth resolveMonth(Integer month, Integer year) {
        if (month == null || year == null) {
            return YearMonth.now();
        }
        if (month < 1 || month > 12) {
            return YearMonth.now();
        }
        return YearMonth.of(year, month);
    }

    @ExceptionHandler(NotFoundException.class)
    public String handleNotFound(@PathVariable Long projectId) {
        return "redirect:/projects/" + projectId;
    }
}
