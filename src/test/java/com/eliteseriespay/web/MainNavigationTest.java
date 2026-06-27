package com.eliteseriespay.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MainNavigationTest {

    @Mock
    private HttpServletRequest request;

    @Test
    void resolveCurrentPath_stripsContextPath() {
        when(request.getRequestURI()).thenReturn("/app/projects");
        when(request.getContextPath()).thenReturn("/app");

        assertThat(MainNavigation.resolveCurrentPath(request)).isEqualTo("/projects");
    }

    @Test
    void resolveCurrentPath_returnsRootForEmptyPathAfterContext() {
        when(request.getRequestURI()).thenReturn("/app");
        when(request.getContextPath()).thenReturn("/app");

        assertThat(MainNavigation.resolveCurrentPath(request)).isEqualTo("/");
    }

    @Test
    void homeActive_onlyForRootPath() {
        assertThat(new MainNavigation("/").homeActive()).isTrue();
        assertThat(new MainNavigation("/projects").homeActive()).isFalse();
    }

    @Test
    void projectsActive_forProjectPaths() {
        MainNavigation navigation = new MainNavigation("/projects/42/payments");

        assertThat(navigation.projectsActive()).isTrue();
        assertThat(navigation.participantsActive()).isFalse();
        assertThat(navigation.homeActive()).isFalse();
    }

    @Test
    void participantsActive_forParticipantPaths() {
        MainNavigation navigation = new MainNavigation("/participants/7/edit");

        assertThat(navigation.participantsActive()).isTrue();
        assertThat(navigation.projectsActive()).isFalse();
    }

    @Test
    void backupsActive_forBackupPaths() {
        MainNavigation navigation = new MainNavigation("/backups");

        assertThat(navigation.backupsActive()).isTrue();
        assertThat(navigation.homeActive()).isFalse();
    }
}
