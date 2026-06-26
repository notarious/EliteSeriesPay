package com.eliteseriespay.service;

import java.math.BigDecimal;

public record NormalizedAmounts(BigDecimal amountOriginal, BigDecimal exchangeRate) {
}
