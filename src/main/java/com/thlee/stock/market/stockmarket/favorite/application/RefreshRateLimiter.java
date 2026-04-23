package com.thlee.stock.market.stockmarket.favorite.application;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalEconomicIndicatorType;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 관심 지표 재조회 레이트 리미터.
 * (userId, indicatorType) 기준 최근 60초 내 1회 허용.
 * 외부 스크래핑 폭주로 인한 Trading Economics 차단을 방지한다.
 */
@Component
public class RefreshRateLimiter {

    private static final long WINDOW_SECONDS = 60L;
    private static final long MAX_ENTRIES = 10_000L;

    private final Cache<String, Long> recentRequests;

    public RefreshRateLimiter() {
        this.recentRequests = Caffeine.newBuilder()
            .expireAfterWrite(WINDOW_SECONDS, TimeUnit.SECONDS)
            .maximumSize(MAX_ENTRIES)
            .build();
    }

    /**
     * @return 토큰 획득 성공이면 true. 이미 window 내에 요청이 있었다면 false.
     */
    public boolean tryAcquire(Long userId, GlobalEconomicIndicatorType indicatorType) {
        String key = userId + ":" + indicatorType.name();
        boolean[] acquired = {false};
        recentRequests.asMap().computeIfAbsent(key, k -> {
            acquired[0] = true;
            return System.currentTimeMillis();
        });
        return acquired[0];
    }
}