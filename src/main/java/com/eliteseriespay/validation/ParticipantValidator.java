package com.eliteseriespay.validation;

import com.eliteseriespay.exception.ValidationException;

public final class ParticipantValidator {

    private ParticipantValidator() {
    }

    public static void validateVkId(String vkId) {
        if (vkId == null || vkId.isBlank()) {
            throw new ValidationException(ValidationError.VK_ID_REQUIRED);
        }
    }

    public static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException(ValidationError.PARTICIPANT_NAME_REQUIRED);
        }
    }
}
