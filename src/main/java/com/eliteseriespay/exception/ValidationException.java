package com.eliteseriespay.exception;

import com.eliteseriespay.validation.ValidationError;

public class ValidationException extends RuntimeException {

    private final ValidationError error;

    public ValidationException(ValidationError error) {
        super(error.getMessage());
        this.error = error;
    }

    public ValidationException(ValidationError error, String message) {
        super(message);
        this.error = error;
    }

    public ValidationError getError() {
        return error;
    }
}
