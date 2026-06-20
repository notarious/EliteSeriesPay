package com.eliteseriespay.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MembershipStatusTest {

    @Test
    void displayName_returnsRussianLabel() {
        assertThat(MembershipStatus.ACTIVE.getDisplayName()).isEqualTo("Активен");
        assertThat(MembershipStatus.LEFT.getDisplayName()).isEqualTo("Исключён");
    }
}
