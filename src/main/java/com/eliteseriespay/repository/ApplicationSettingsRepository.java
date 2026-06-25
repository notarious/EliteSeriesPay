package com.eliteseriespay.repository;

import com.eliteseriespay.domain.ApplicationSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationSettingsRepository extends JpaRepository<ApplicationSettings, Long> {
}
