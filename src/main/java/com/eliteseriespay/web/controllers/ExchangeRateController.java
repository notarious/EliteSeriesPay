package com.eliteseriespay.web.controllers;

import com.eliteseriespay.domain.PaymentCurrency;
import com.eliteseriespay.exchangerate.ExchangeRateFetchException;
import com.eliteseriespay.exchangerate.ExchangeRateQuote;
import com.eliteseriespay.exception.ValidationException;
import com.eliteseriespay.service.ExchangeRateService;
import com.eliteseriespay.web.dto.ApiErrorResponse;
import com.eliteseriespay.web.dto.ExchangeRateResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/exchange-rates")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    public ExchangeRateController(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    @GetMapping
    public ExchangeRateResponse getRate(@RequestParam PaymentCurrency currency) {
        ExchangeRateQuote quote = exchangeRateService.getRateToRub(currency);
        return new ExchangeRateResponse(quote.currency().name(), quote.rate());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(ValidationException ex) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ExchangeRateFetchException.class)
    public ResponseEntity<ApiErrorResponse> handleFetchFailure(ExchangeRateFetchException ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : ExchangeRateFetchException.USER_MESSAGE;
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiErrorResponse(message));
    }
}
