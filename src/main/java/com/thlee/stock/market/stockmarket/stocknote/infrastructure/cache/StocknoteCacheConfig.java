package com.thlee.stock.market.stockmarket.stocknote.infrastructure.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * stocknote 도메인 전용 Caffeine 캐시.
 *
 * <p>대시보드 KPI 집계 캐시. 심화 E 권고에 따라 TTL 30분 (기록 CUD 시 evict 로 즉시 무효화).
 */
@Configuration
public class StocknoteCacheConfig {

    public static final String DASHBOARD_CACHE_NAME = "stocknoteDashboard";

    @Bean("stocknoteCacheManager")
    public CacheManager stocknoteCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(DASHBOARD_CACHE_NAME);
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(1000));
        return manager;
    }
}