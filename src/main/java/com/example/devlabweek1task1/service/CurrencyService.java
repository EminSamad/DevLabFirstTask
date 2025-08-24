package com.example.devlabweek1task1.service;

import com.example.devlabweek1task1.model.ExchangeRateResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class CurrencyService {

    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${currency.api.base-url}")
    private String apiBaseUrl;

    @Value("${retry.max-attempts}")
    private int maxAttempts;

    @Value("${retry.backoff-period}")
    private long backoffPeriod;

    private static final String REDIS_RATES_KEY = "exchangeRates:USD";

    public CurrencyService(RestTemplate restTemplate, RedisTemplate<String, Object> redisTemplate) {
        this.restTemplate = restTemplate;
        this.redisTemplate = redisTemplate;
    }


    @Scheduled(cron = "${currency.update.cron}")
    public void updateRatesScheduled() {
        fetchAndCacheRates();
    }

    public void fetchAndCacheRates() {
        Map<String, Double> newRates = getLatestRatesWithRetryAndFallback();

        if (newRates != null && !newRates.isEmpty()) {
            redisTemplate.opsForHash().putAll(REDIS_RATES_KEY, newRates);
            redisTemplate.expire(REDIS_RATES_KEY, 2, TimeUnit.HOURS);
            System.out.println("CACHE: Yeni məzənnələr Redis-də yaddaşlandı.");
        }
    }

    @Retryable(
            value = { RestClientException.class },
            maxAttemptsExpression = "#{${retry.max-attempts}}",
            backoff = @Backoff(delayExpression = "#{${retry.backoff-period}}")
    )
    public Map<String, Double> getLatestRatesWithRetry() {
        System.out.println("API CƏHDİ: Valyuta məzənnələrini API-dən çəkir.");

        String fullUrl = apiBaseUrl + "USD";
        ResponseEntity<ExchangeRateResponse> response =
                restTemplate.getForEntity(fullUrl, ExchangeRateResponse.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody().getRates();
        } else {
            throw new RestClientException("API-dən uğursuz cavab: " + response.getStatusCode());
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Double> getCachedRates() {
        System.out.println("FALLBACK: Məzənnələr Redis Cache-dən oxunur.");
        return (Map<String, Double>) (Map<?, ?>) redisTemplate.opsForHash().entries(REDIS_RATES_KEY);
    }

    public Map<String, Double> getLatestRatesWithRetryAndFallback() {
        try {
            return getLatestRatesWithRetry();
        } catch (Exception apiException) {
            System.err.println("XƏTA: Bütün API cəhdləri uğursuz oldu. Keçid Cache-ə.");
            Map<String, Double> cachedRates = getCachedRates();
            if (cachedRates != null && !cachedRates.isEmpty()) {
                return cachedRates;
            } else {
                System.err.println("KRİTİK XƏTA: Cache boşdur. Servis məlumat təmin edə bilmir.");
                return null;
            }
        }
    }

    public Double convertCurrency(double amount, String targetCurrency) {
        Map<String, Double> rates = getLatestRatesWithRetryAndFallback();

        if (rates == null) {
            return null; //
        }

        String key = targetCurrency.toUpperCase();
        if (rates.containsKey(key)) {
            double rate = rates.get(key);
            return amount * rate;
        } else {
            System.err.println("XƏTA: Təyin olunmuş valyuta (" + targetCurrency + ") məzənnələrdə tapılmadı.");
            return null;
        }
    }
}