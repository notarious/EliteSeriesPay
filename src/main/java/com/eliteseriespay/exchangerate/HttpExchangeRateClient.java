package com.eliteseriespay.exchangerate;

import com.eliteseriespay.domain.PaymentCurrency;
import java.math.BigDecimal;
import java.net.URI;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class HttpExchangeRateClient implements ExchangeRateClient {

    private static final String TARGET_CURRENCY = "RUB";

    private final RestClient restClient;
    private final ExchangeRateProperties properties;

    public HttpExchangeRateClient(@Qualifier("exchangeRateRestClientBuilder") RestClient.Builder restClientBuilder,
                                  ExchangeRateProperties properties) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public ExchangeRateQuote fetchRateToRub(PaymentCurrency currency) {
        URI uri = UriComponentsBuilder.fromUriString(properties.getApiUrl())
                .queryParam("from", currency.name())
                .queryParam("to", TARGET_CURRENCY)
                .build()
                .toUri();

        try {
            FrankfurterResponse response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(FrankfurterResponse.class);

            BigDecimal rate = extractRubRate(response);
            return new ExchangeRateQuote(currency, rate);
        } catch (RestClientException ex) {
            throw new ExchangeRateFetchException("Exchange rate request failed for " + currency, ex);
        }
    }

    private BigDecimal extractRubRate(FrankfurterResponse response) {
        if (response == null || response.rates() == null) {
            throw new ExchangeRateFetchException("Exchange rate response is empty");
        }

        BigDecimal rate = response.rates().get(TARGET_CURRENCY);
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ExchangeRateFetchException("Exchange rate response does not contain a valid RUB rate");
        }

        return rate;
    }
}
