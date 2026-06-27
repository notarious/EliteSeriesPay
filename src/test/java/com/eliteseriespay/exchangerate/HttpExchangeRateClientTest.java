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
import com.fasterxml.jackson.databind.ObjectMapper;

class HttpExchangeRateClientTest {

    private static final String CBR_API_URL = "https://www.cbr-xml-daily.ru/daily_json.js";
    private static final MediaType CBR_MEDIA_TYPE = MediaType.parseMediaType("application/javascript;charset=utf-8");

    private ExchangeRateProperties properties;
    private MockRestServiceServer server;
    private HttpExchangeRateClient client;

    @BeforeEach
    void setUp() {
        properties = new ExchangeRateProperties();
        properties.setApiUrl(CBR_API_URL);

        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HttpExchangeRateClient(builder, properties, new ObjectMapper());
    }

    @Test
    void fetchRateToRub_returnsUsdRate() {
        server.expect(requestTo(CBR_API_URL))
                .andRespond(withSuccess("""
                        {"Valute":{"USD":{"Nominal":1,"Value":77.0611}}}
                        """, CBR_MEDIA_TYPE));

        ExchangeRateQuote quote = client.fetchRateToRub(PaymentCurrency.USD);

        assertThat(quote.currency()).isEqualTo(PaymentCurrency.USD);
        assertThat(quote.rate()).isEqualByComparingTo("77.0611");
        server.verify();
    }

    @Test
    void fetchRateToRub_returnsEurRate() {
        server.expect(requestTo(CBR_API_URL))
                .andRespond(withSuccess("""
                        {"Valute":{"EUR":{"Nominal":1,"Value":87.4027}}}
                        """, CBR_MEDIA_TYPE));

        ExchangeRateQuote quote = client.fetchRateToRub(PaymentCurrency.EUR);

        assertThat(quote.currency()).isEqualTo(PaymentCurrency.EUR);
        assertThat(quote.rate()).isEqualByComparingTo("87.4027");
        server.verify();
    }

    @Test
    void fetchRateToRub_normalizesNominal() {
        server.expect(requestTo(CBR_API_URL))
                .andRespond(withSuccess("""
                        {"Valute":{"USD":{"Nominal":10,"Value":770.611}}}
                        """, CBR_MEDIA_TYPE));

        ExchangeRateQuote quote = client.fetchRateToRub(PaymentCurrency.USD);

        assertThat(quote.rate()).isEqualByComparingTo("77.0611");
        server.verify();
    }

    @Test
    void fetchRateToRub_rejectsInvalidResponse() {
        server.expect(requestTo(CBR_API_URL))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchRateToRub(PaymentCurrency.USD))
                .isInstanceOf(ExchangeRateFetchException.class)
                .hasMessageContaining("empty");
        server.verify();
    }

    @Test
    void fetchRateToRub_wrapsTimeoutAsFetchFailure() {
        server.expect(requestTo(CBR_API_URL))
                .andRespond(withException(new IOException(new SocketTimeoutException("timeout"))));

        assertThatThrownBy(() -> client.fetchRateToRub(PaymentCurrency.USD))
                .isInstanceOf(ExchangeRateFetchException.class)
                .hasMessageContaining("Exchange rate request failed");
        server.verify();
    }
}
