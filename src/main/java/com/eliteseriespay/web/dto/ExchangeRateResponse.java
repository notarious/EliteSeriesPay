package com.eliteseriespay.web.dto;

import java.math.BigDecimal;

public record ExchangeRateResponse(String currency, BigDecimal rate) {
}
