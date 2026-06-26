package com.eliteseriespay.web.controllers;

import com.eliteseriespay.service.ApplicationResetService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/reset-data")
public class ApplicationResetController {

    static final String SUCCESS_MESSAGE = "Все данные успешно удалены.";

    private final ApplicationResetService applicationResetService;

    public ApplicationResetController(ApplicationResetService applicationResetService) {
        this.applicationResetService = applicationResetService;
    }

    @PostMapping
    public String reset(RedirectAttributes redirectAttributes) {
        applicationResetService.resetAllData();
        redirectAttributes.addFlashAttribute("successMessage", SUCCESS_MESSAGE);
        return "redirect:/";
    }
}
