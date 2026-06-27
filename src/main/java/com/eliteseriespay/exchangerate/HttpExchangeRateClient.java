package com.eliteseriespay.exchangerate;

import com.eliteseriespay.domain.PaymentCurrency;
import com.eliteseriespay.exchangerate.CbrDailyResponse.CbrValute;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpExchangeRateClient implements ExchangeRateClient {

    private final RestClient restClient;
    private final ExchangeRateProperties properties;
    private final ObjectMapper objectMapper;

    public HttpExchangeRateClient(@Qualifier("exchangeRateRestClientBuilder") RestClient.Builder restClientBuilder,
                                  ExchangeRateProperties properties,
                                  ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    @Override
    public ExchangeRateQuote fetchRateToRub(PaymentCurrency currency) {
        try {
            String responseBody = restClient.get()
                    .uri(properties.getApiUrl())
                    .retrieve()
                    .body(String.class);

            CbrDailyResponse response = objectMapper.readValue(responseBody, CbrDailyResponse.class);
            BigDecimal rate = extractRubRate(response, currency);
            return new ExchangeRateQuote(currency, rate);
        } catch (RestClientException ex) {
            throw new ExchangeRateFetchException("Exchange rate request failed for " + currency, ex);
        } catch (JsonProcessingException ex) {
            throw new ExchangeRateFetchException("Exchange rate response is invalid", ex);
        }
    }

    private BigDecimal extractRubRate(CbrDailyResponse response, PaymentCurrency currency) {
        if (response == null || response.valute() == null) {
            throw new ExchangeRateFetchException("Exchange rate response is empty");
        }

        CbrValute valute = response.valute().get(currency.name());
        if (valute == null || valute.nominal() <= 0 || valute.value() == null
                || valute.value().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ExchangeRateFetchException("Exchange rate response does not contain a valid RUB rate");
        }

        return valute.value().divide(BigDecimal.valueOf(valute.nominal()), 10, RoundingMode.HALF_UP);
    }
}
