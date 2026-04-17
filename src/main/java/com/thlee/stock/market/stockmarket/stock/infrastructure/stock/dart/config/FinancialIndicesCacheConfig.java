package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class FinancialIndicesCacheConfig {

    public static final String FINANCIAL_INDICES_CACHE = "financialIndices";
    public static final String CACHE_MANAGER_NAME = "financialIndicesCacheManager";

    @Bean(name = CACHE_MANAGER_NAME)
    public CacheManager financialIndicesCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(FINANCIAL_INDICES_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .maximumSize(500));
        return cacheManager;
    }
}
