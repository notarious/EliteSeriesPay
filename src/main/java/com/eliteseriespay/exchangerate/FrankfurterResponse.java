package com.eliteseriespay.exchangerate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
record FrankfurterResponse(Map<String, BigDecimal> rates) {
}
