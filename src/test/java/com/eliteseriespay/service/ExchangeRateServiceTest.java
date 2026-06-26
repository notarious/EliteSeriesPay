package com.eliteseriespay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eliteseriespay.domain.PaymentCurrency;
import com.eliteseriespay.exchangerate.ExchangeRateClient;
import com.eliteseriespay.exchangerate.ExchangeRateFetchException;
import com.eliteseriespay.exchangerate.ExchangeRateQuote;
import com.eliteseriespay.exception.ValidationException;
import com.eliteseriespay.validation.ValidationError;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock
    private ExchangeRateClient exchangeRateClient;

    private ExchangeRateService exchangeRateService;

    @BeforeEach
    void setUp() {
        exchangeRateService = new ExchangeRateService(exchangeRateClient);
    }

    @Test
    void getRateToRub_fetchesUsdRate() {
        when(exchangeRateClient.fetchRateToRub(PaymentCurrency.USD))
                .thenReturn(new ExchangeRateQuote(PaymentCurrency.USD, new BigDecimal("90.55555")));

        ExchangeRateQuote quote = exchangeRateService.getRateToRub(PaymentCurrency.USD);

        assertThat(quote.currency()).isEqualTo(PaymentCurrency.USD);
        assertThat(quote.rate()).isEqualByComparingTo("90.5556");
        verify(exchangeRateClient).fetchRateToRub(PaymentCurrency.USD);
    }

    @Test
    void getRateToRub_fetchesEurRate() {
        when(exchangeRateClient.fetchRateToRub(PaymentCurrency.EUR))
                .thenReturn(new ExchangeRateQuote(PaymentCurrency.EUR, new BigDecimal("98.12")));

        ExchangeRateQuote quote = exchangeRateService.getRateToRub(PaymentCurrency.EUR);

        assertThat(quote.currency()).isEqualTo(PaymentCurrency.EUR);
        assertThat(quote.rate()).isEqualByComparingTo("98.1200");
        verify(exchangeRateClient).fetchRateToRub(PaymentCurrency.EUR);
    }

    @Test
    void getRateToRub_rejectsRubCurrency() {
        assertThatThrownBy(() -> exchangeRateService.getRateToRub(PaymentCurrency.RUB))
                .isInstanceOf(ValidationException.class)
                .extracting(ex -> ((ValidationException) ex).getError())
                .isEqualTo(ValidationError.EXCHANGE_RATE_UNSUPPORTED_CURRENCY);
    }

    @Test
    void getRateToRub_rejectsNullCurrency() {
        assertThatThrownBy(() -> exchangeRateService.getRateToRub(null))
                .isInstanceOf(ValidationException.class)
                .extracting(ex -> ((ValidationException) ex).getError())
                .isEqualTo(ValidationError.EXCHANGE_RATE_UNSUPPORTED_CURRENCY);
    }

    @Test
    void getRateToRub_wrapsExternalApiFailure() {
        when(exchangeRateClient.fetchRateToRub(PaymentCurrency.USD))
                .thenThrow(new ExchangeRateFetchException("upstream failed"));

        assertThatThrownBy(() -> exchangeRateService.getRateToRub(PaymentCurrency.USD))
                .isInstanceOf(ExchangeRateFetchException.class)
                .hasMessage(ExchangeRateFetchException.USER_MESSAGE);
    }

    @Test
    void getRateToRub_returnsUserFriendlyErrorOnTimeout() {
        when(exchangeRateClient.fetchRateToRub(PaymentCurrency.EUR))
                .thenThrow(new ExchangeRateFetchException("Exchange rate request failed for EUR",
                        new java.io.IOException(new java.net.SocketTimeoutException("timeout"))));

        assertThatThrownBy(() -> exchangeRateService.getRateToRub(PaymentCurrency.EUR))
                .isInstanceOf(ExchangeRateFetchException.class)
                .hasMessage(ExchangeRateFetchException.USER_MESSAGE);
    }
}
