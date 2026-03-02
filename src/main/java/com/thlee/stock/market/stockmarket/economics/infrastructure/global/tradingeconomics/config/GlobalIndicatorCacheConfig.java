package com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class GlobalIndicatorCacheConfig {

    public static final String GLOBAL_INDICATOR_CACHE = "globalIndicators";

    @Bean
    public CacheManager globalIndicatorCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(GLOBAL_INDICATOR_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(12, TimeUnit.HOURS)
            .maximumSize(200));
        return cacheManager;
    }
}