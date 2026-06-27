package com.eliteseriespay.desktop;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.mock.env.MockEnvironment;

class DesktopEnvironmentTest {

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void isTrayEnabled_usesPackagedWindowsByDefault() {
        MockEnvironment environment = packagedWindowsEnvironment();
        DesktopProperties properties = new DesktopProperties();

        assertThat(DesktopEnvironment.isTrayEnabled(properties, environment)).isTrue();
    }

    @Test
    void isTrayEnabled_canBeDisabledExplicitly() {
        MockEnvironment environment = packagedWindowsEnvironment();
        DesktopProperties properties = new DesktopProperties();
        properties.setTrayEnabled(false);

        assertThat(DesktopEnvironment.isTrayEnabled(properties, environment)).isFalse();
    }

    @Test
    void isTrayEnabled_disabledForDevelopment() {
        MockEnvironment environment = new MockEnvironment();
        DesktopProperties properties = new DesktopProperties();

        assertThat(DesktopEnvironment.isTrayEnabled(properties, environment)).isFalse();
    }

    @Test
    void applicationUrl_usesConfiguredServerPort() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("server.port", "9090");

        assertThat(DesktopEnvironment.applicationUrl(environment)).isEqualTo("http://localhost:9090");
    }

    private static MockEnvironment packagedWindowsEnvironment() {
        return new MockEnvironment()
                .withProperty("eliteseriespay.packaged", "true")
                .withProperty("LOCALAPPDATA", "C:\\Users\\test\\AppData\\Local")
                .withProperty("os.name", "Windows 10");
    }
}
