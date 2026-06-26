package com.eliteseriespay.web.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eliteseriespay.domain.PaymentCurrency;
import com.eliteseriespay.exchangerate.ExchangeRateClient;
import com.eliteseriespay.exchangerate.ExchangeRateFetchException;
import com.eliteseriespay.exchangerate.ExchangeRateQuote;
import com.eliteseriespay.service.ExchangeRateService;
import com.eliteseriespay.validation.ValidationError;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ExchangeRateControllerTest {

    @Mock
    private ExchangeRateClient exchangeRateClient;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ExchangeRateService exchangeRateService = new ExchangeRateService(exchangeRateClient);
        mockMvc = MockMvcBuilders.standaloneSetup(new ExchangeRateController(exchangeRateService)).build();
    }

    @Test
    void getRate_returnsUsdRate() throws Exception {
        when(exchangeRateClient.fetchRateToRub(PaymentCurrency.USD))
                .thenReturn(new ExchangeRateQuote(PaymentCurrency.USD, new BigDecimal("90.55555")));

        mockMvc.perform(get("/exchange-rates").param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.rate").value(90.5556));
    }

    @Test
    void getRate_returnsEurRate() throws Exception {
        when(exchangeRateClient.fetchRateToRub(PaymentCurrency.EUR))
                .thenReturn(new ExchangeRateQuote(PaymentCurrency.EUR, new BigDecimal("98.12")));

        mockMvc.perform(get("/exchange-rates").param("currency", "EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.rate").value(98.1200));
    }

    @Test
    void getRate_rejectsUnsupportedCurrency() throws Exception {
        mockMvc.perform(get("/exchange-rates").param("currency", "RUB"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ValidationError.EXCHANGE_RATE_UNSUPPORTED_CURRENCY.getMessage()));
    }

    @Test
    void getRate_returnsUserFriendlyErrorWhenFetchFails() throws Exception {
        when(exchangeRateClient.fetchRateToRub(PaymentCurrency.USD))
                .thenThrow(new ExchangeRateFetchException("Exchange rate request failed for USD",
                        new IOException(new SocketTimeoutException("timeout"))));

        mockMvc.perform(get("/exchange-rates").param("currency", "USD"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(ExchangeRateFetchException.USER_MESSAGE));
    }
}
