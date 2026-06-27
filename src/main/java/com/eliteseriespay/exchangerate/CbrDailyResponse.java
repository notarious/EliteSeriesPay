package com.eliteseriespay.exchangerate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
record CbrDailyResponse(@JsonProperty("Valute") Map<String, CbrValute> valute) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CbrValute(@JsonProperty("Nominal") int nominal, @JsonProperty("Value") BigDecimal value) {
    }
}
