package com.eliteseriespay.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public record MainNavigation(String currentPath) {

    public static MainNavigation from(HttpServletRequest request) {
        return new MainNavigation(resolveCurrentPath(request));
    }

    static String resolveCurrentPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && requestUri.startsWith(contextPath)) {
            requestUri = requestUri.substring(contextPath.length());
        }
        if (!StringUtils.hasText(requestUri)) {
            return "/";
        }
        return requestUri;
    }

    public boolean homeActive() {
        return "/".equals(currentPath);
    }

    public boolean projectsActive() {
        return currentPath.startsWith("/projects");
    }

    public boolean participantsActive() {
        return currentPath.startsWith("/participants");
    }

    public boolean backupsActive() {
        return currentPath.startsWith("/backups");
    }
}
