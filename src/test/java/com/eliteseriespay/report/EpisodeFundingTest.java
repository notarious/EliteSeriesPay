package com.eliteseriespay.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class EpisodeFundingTest {

    @Test
    void calculate_episodeFundingWithRemainder() {
        EpisodeFunding funding = EpisodeFunding.calculate(
                new BigDecimal("31800.00"), new BigDecimal("4500.00"));

        assertThat(funding.episodesFunded()).isEqualTo(7);
        assertThat(funding.remainingRub()).isEqualByComparingTo("300.00");
        assertThat(funding.amountToNextEpisode()).isEqualByComparingTo("4200.00");
    }

    @Test
    void calculate_episodeFundingWithoutRemainder() {
        EpisodeFunding funding = EpisodeFunding.calculate(
                new BigDecimal("31500.00"), new BigDecimal("4500.00"));

        assertThat(funding.episodesFunded()).isEqualTo(7);
        assertThat(funding.remainingRub()).isEqualByComparingTo("0.00");
        assertThat(funding.amountToNextEpisode()).isEqualByComparingTo("4500.00");
    }

    @Test
    void calculate_noPayments() {
        EpisodeFunding funding = EpisodeFunding.calculate(
                BigDecimal.ZERO.setScale(2), new BigDecimal("4500.00"));

        assertThat(funding.episodesFunded()).isZero();
        assertThat(funding.remainingRub()).isEqualByComparingTo("0.00");
        assertThat(funding.amountToNextEpisode()).isEqualByComparingTo("4500.00");
    }
}
