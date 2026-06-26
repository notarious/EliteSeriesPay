package com.eliteseriespay.report;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record EpisodeFunding(int episodesFunded,
                             BigDecimal remainingRub,
                             BigDecimal amountToNextEpisode) {

    public static EpisodeFunding calculate(BigDecimal totalNetRub, BigDecimal episodeCostRub) {
        int episodesFunded = totalNetRub.divideToIntegralValue(episodeCostRub).intValue();
        BigDecimal spentOnEpisodes = episodeCostRub.multiply(BigDecimal.valueOf(episodesFunded));
        BigDecimal remainingRub = totalNetRub.subtract(spentOnEpisodes).setScale(2, RoundingMode.HALF_UP);
        BigDecimal amountToNextEpisode = episodeCostRub.subtract(remainingRub).setScale(2, RoundingMode.HALF_UP);
        return new EpisodeFunding(episodesFunded, remainingRub, amountToNextEpisode);
    }
}
