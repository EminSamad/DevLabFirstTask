package com.example.devlabweek1task1.model;

import java.util.Map;
import lombok.Data;

@Data
public class ExchangeRateResponse {

    private String base;
    private Map<String, Double> rates;
}