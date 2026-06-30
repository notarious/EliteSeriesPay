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

    @Transactional
    public ApplicationSettings getSettings() {
        return applicationSettingsRepository.findById(ApplicationSettings.SINGLETON_ID)
                .orElseGet(() -> applicationSettingsRepository.save(
                        new ApplicationSettings(ApplicationSettings.DEFAULT_VK_DONUT_FEE_PERCENT)));
    }

    @Transactional
    public int getVkDonutFeePercent() {
        return getSettings().getVkDonutFeePercent();
    }
}
