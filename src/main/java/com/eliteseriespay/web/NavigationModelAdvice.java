package com.eliteseriespay.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class NavigationModelAdvice {

    @ModelAttribute("navigation")
    public MainNavigation navigation(HttpServletRequest request) {
        return MainNavigation.from(request);
    }
}
