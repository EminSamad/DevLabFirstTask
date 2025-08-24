package com.example.devlabweek1task1.controller;

import com.example.devlabweek1task1.service.CurrencyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/convert")
public class CurrencyController {

    private final CurrencyService currencyService;

    public CurrencyController(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    @GetMapping
    public ResponseEntity<?> convert(
            @RequestParam double amount,
            @RequestParam String to) {

        if (amount <= 0) {
            return new ResponseEntity<>("Məbləğ sıfırdan böyük olmalıdır.", HttpStatus.BAD_REQUEST);
        }

        Double convertedAmount = currencyService.convertCurrency(amount, to.toUpperCase());

        if (convertedAmount != null) {
            String responseMessage = String.format("%.2f USD = %.2f %s", amount, convertedAmount, to.toUpperCase());
            return new ResponseEntity<>(responseMessage, HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Xidmət hazırda məlumat təmin edə bilmir. Zəhmət olmasa, bir az sonra yenidən cəhd edin.", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}