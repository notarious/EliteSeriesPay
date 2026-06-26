package com.eliteseriespay.payment;

import java.math.BigDecimal;

public record NormalizedAmounts(BigDecimal amountOriginal, BigDecimal exchangeRate) {
}
