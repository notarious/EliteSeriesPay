package com.eliteseriespay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eliteseriespay.domain.ApplicationSettings;
import com.eliteseriespay.repository.ApplicationSettingsRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationSettingsServiceTest {

    @Mock
    private ApplicationSettingsRepository applicationSettingsRepository;

    private ApplicationSettingsService applicationSettingsService;

    @BeforeEach
    void setUp() {
        applicationSettingsService = new ApplicationSettingsService(applicationSettingsRepository);
    }

    @Test
    void getVkDonutFeePercent_returnsValueFromSettings() {
        when(applicationSettingsRepository.findById(ApplicationSettings.SINGLETON_ID))
                .thenReturn(Optional.of(new ApplicationSettings(15)));

        assertThat(applicationSettingsService.getVkDonutFeePercent()).isEqualTo(15);
    }

    @Test
    void getSettings_createsDefaultSettingsWhenMissing() {
        when(applicationSettingsRepository.findById(ApplicationSettings.SINGLETON_ID))
                .thenReturn(Optional.empty());
        when(applicationSettingsRepository.save(any(ApplicationSettings.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ApplicationSettings settings = applicationSettingsService.getSettings();

        assertThat(settings.getVkDonutFeePercent())
                .isEqualTo(ApplicationSettings.DEFAULT_VK_DONUT_FEE_PERCENT);
        verify(applicationSettingsRepository).save(any(ApplicationSettings.class));
    }
}
