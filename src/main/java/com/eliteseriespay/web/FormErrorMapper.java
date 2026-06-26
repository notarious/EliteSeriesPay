package com.eliteseriespay.web;

import com.eliteseriespay.exception.ValidationException;
import com.eliteseriespay.validation.ValidationError;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;

@Component
public class FormErrorMapper {

    public void rejectProjectForm(BindingResult bindingResult, ValidationException ex) {
        ValidationError error = ex.getError();
        String field = switch (error) {
            case PROJECT_NAME_REQUIRED -> "name";
            case EPISODE_COST_REQUIRED, EPISODE_COST_NOT_POSITIVE -> "episodeCostRub";
            case MONTHLY_FEE_RUB_REQUIRED, MONTHLY_FEE_RUB_NOT_POSITIVE -> "monthlyFeeRub";
            case MONTHLY_FEE_EUR_REQUIRED, MONTHLY_FEE_EUR_NOT_POSITIVE -> "monthlyFeeEur";
            default -> throw unexpected(error);
        };
        reject(bindingResult, field, error);
    }

    public void rejectParticipantForm(BindingResult bindingResult, ValidationException ex) {
        ValidationError error = ex.getError();
        switch (error) {
            case VK_ID_REQUIRED, VK_ID_ALREADY_EXISTS -> reject(bindingResult, "vkId", error);
            case PARTICIPANT_NAME_REQUIRED -> reject(bindingResult, "name", error);
            default -> throw unexpected(error);
        }
    }

    public void rejectAddToProjectForm(BindingResult bindingResult, ValidationException ex) {
        ValidationError error = ex.getError();
        if (error == ValidationError.PARTICIPANT_ALREADY_ACTIVE) {
            reject(bindingResult, "projectId", error);
            return;
        }
        if (error == ValidationError.BILLING_MODE_REQUIRED) {
            reject(bindingResult, "billingMode", error);
            return;
        }
        throw unexpected(error);
    }

    public void rejectExistingParticipantForm(BindingResult bindingResult, ValidationException ex) {
        ValidationError error = ex.getError();
        switch (error) {
            case PARTICIPANT_ALREADY_ACTIVE -> reject(bindingResult, "participantId", error);
            case BILLING_MODE_REQUIRED -> reject(bindingResult, "billingMode", error);
            default -> throw unexpected(error);
        }
    }

    public void rejectProjectParticipantForm(BindingResult bindingResult, ValidationException ex) {
        ValidationError error = ex.getError();
        switch (error) {
            case VK_ID_REQUIRED, VK_ID_ALREADY_EXISTS, PARTICIPANT_ALREADY_ACTIVE ->
                    reject(bindingResult, "vkId", error);
            case PARTICIPANT_NAME_REQUIRED, NOT_AN_ACTIVE_MEMBER ->
                    reject(bindingResult, "name", error);
            case BILLING_MODE_REQUIRED -> reject(bindingResult, "billingMode", error);
            default -> throw unexpected(error);
        }
    }

    private void reject(BindingResult bindingResult, String field, ValidationError error) {
        bindingResult.rejectValue(field, error.name(), error.getMessage());
    }

    private IllegalStateException unexpected(ValidationError error) {
        return new IllegalStateException("Unexpected validation error: " + error);
    }
}
