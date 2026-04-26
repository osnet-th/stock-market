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
    public static final long STOCK_PRICE_CACHE_TTL_MINUTES = 30;

    /** 일봉(immutable 시계열) 캐시. 종가 확정 후 변하지 않으므로 long TTL (12h). */
    public static final String DAILY_HISTORY_CACHE = "stockDailyHistory";
    public static final long DAILY_HISTORY_TTL_HOURS = 12;

    @Bean
    public CacheManager stockPriceCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(STOCK_PRICE_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(STOCK_PRICE_CACHE_TTL_MINUTES, TimeUnit.MINUTES)
            .maximumSize(500));
        return cacheManager;
    }

    @Bean
    public CacheManager dailyHistoryCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(DAILY_HISTORY_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(DAILY_HISTORY_TTL_HOURS, TimeUnit.HOURS)
            .maximumSize(200));
        return cacheManager;
    }
}