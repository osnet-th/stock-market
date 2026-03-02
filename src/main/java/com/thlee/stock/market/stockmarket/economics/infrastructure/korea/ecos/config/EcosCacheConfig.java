package com.thlee.stock.market.stockmarket.economics.infrastructure.korea.ecos.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class EcosCacheConfig {

    public static final String ECOS_INDICATOR_CACHE = "ecosIndicators";

    @Bean
    @Primary
    public CacheManager ecosCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(ECOS_INDICATOR_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(25, TimeUnit.HOURS) // 배치 주기(24h)보다 약간 길게
            .maximumSize(15));
        return cacheManager;
    }
}