package com.eliteseriespay.service;

import com.eliteseriespay.domain.ApplicationSettings;
import com.eliteseriespay.repository.ApplicationSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationSettingsService {

    private final ApplicationSettingsRepository applicationSettingsRepository;

    public ApplicationSettingsService(ApplicationSettingsRepository applicationSettingsRepository) {
        this.applicationSettingsRepository = applicationSettingsRepository;
    }

    @Transactional(readOnly = true)
    public ApplicationSettings getSettings() {
        return applicationSettingsRepository.findById(ApplicationSettings.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("Application settings not initialized"));
    }

    @Transactional(readOnly = true)
    public int getVkDonutFeePercent() {
        return getSettings().getVkDonutFeePercent();
    }
}
