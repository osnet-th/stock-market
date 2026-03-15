package com.thlee.stock.market.stockmarket.stock.infrastructure.exchangerate.koreaexim;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class ExchangeRateCacheConfig {

    @Bean
    public CacheManager exchangeRateCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("exchangeRate");
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(20));
        return cacheManager;
    }
}