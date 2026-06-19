package com.eliteseriespay.service;

public class ProjectValidationException extends RuntimeException {

    private final ProjectValidationError error;

    public ProjectValidationException(ProjectValidationError error) {
        super(error.getMessage());
        this.error = error;
    }

    public ProjectValidationError getError() {
        return error;
    }
}
