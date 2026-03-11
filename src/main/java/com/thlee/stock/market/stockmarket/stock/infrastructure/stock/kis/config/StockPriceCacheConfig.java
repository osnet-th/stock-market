package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class StockPriceCacheConfig {

    public static final String STOCK_PRICE_CACHE = "stockPrice";

    @Bean
    public CacheManager stockPriceCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(STOCK_PRICE_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .maximumSize(500));
        return cacheManager;
    }
}