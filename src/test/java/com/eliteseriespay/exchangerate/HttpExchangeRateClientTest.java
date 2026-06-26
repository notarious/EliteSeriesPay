package com.eliteseriespay.exchangerate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.eliteseriespay.domain.PaymentCurrency;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpExchangeRateClientTest {

    private ExchangeRateProperties properties;
    private MockRestServiceServer server;
    private HttpExchangeRateClient client;

    @BeforeEach
    void setUp() {
        properties = new ExchangeRateProperties();
        properties.setApiUrl("https://api.example.test/latest");

        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HttpExchangeRateClient(builder, properties);
    }

    @Test
    void fetchRateToRub_returnsUsdRate() {
        server.expect(requestTo("https://api.example.test/latest?from=USD&to=RUB"))
                .andRespond(withSuccess("""
                        {"amount":1.0,"base":"USD","date":"2026-06-26","rates":{"RUB":90.5}}
                        """, MediaType.APPLICATION_JSON));

        ExchangeRateQuote quote = client.fetchRateToRub(PaymentCurrency.USD);

        assertThat(quote.currency()).isEqualTo(PaymentCurrency.USD);
        assertThat(quote.rate()).isEqualByComparingTo("90.5");
        server.verify();
    }

    @Test
    void fetchRateToRub_returnsEurRate() {
        server.expect(requestTo("https://api.example.test/latest?from=EUR&to=RUB"))
                .andRespond(withSuccess("""
                        {"amount":1.0,"base":"EUR","date":"2026-06-26","rates":{"RUB":98.12}}
                        """, MediaType.APPLICATION_JSON));

        ExchangeRateQuote quote = client.fetchRateToRub(PaymentCurrency.EUR);

        assertThat(quote.currency()).isEqualTo(PaymentCurrency.EUR);
        assertThat(quote.rate()).isEqualByComparingTo("98.12");
        server.verify();
    }

    @Test
    void fetchRateToRub_rejectsInvalidResponse() {
        server.expect(requestTo("https://api.example.test/latest?from=USD&to=RUB"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchRateToRub(PaymentCurrency.USD))
                .isInstanceOf(ExchangeRateFetchException.class)
                .hasMessageContaining("empty");
        server.verify();
    }

    @Test
    void fetchRateToRub_wrapsTimeoutAsFetchFailure() {
        server.expect(requestTo("https://api.example.test/latest?from=USD&to=RUB"))
                .andRespond(withException(new IOException(new SocketTimeoutException("timeout"))));

        assertThatThrownBy(() -> client.fetchRateToRub(PaymentCurrency.USD))
                .isInstanceOf(ExchangeRateFetchException.class)
                .hasMessageContaining("Exchange rate request failed");
        server.verify();
    }
}
