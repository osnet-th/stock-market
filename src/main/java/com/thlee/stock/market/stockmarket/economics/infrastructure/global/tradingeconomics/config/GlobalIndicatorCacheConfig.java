package com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics.config;

import java.time.Duration;

/**
 * 글로벌 지표 캐시 정책 상수.
 * Caffeine {@code LoadingCache} 를 {@link com.thlee.stock.market.stockmarket.economics.application.GlobalIndicatorCacheService}
 * 가 내부에서 직접 빌드하므로 별도 {@code CacheManager} bean 은 두지 않는다 (Rev.2).
 */
public final class GlobalIndicatorCacheConfig {

    public static final String GLOBAL_INDICATOR_CACHE = "globalIndicators";
    public static final Duration TTL = Duration.ofHours(12);
    /** 빈 결과는 짧게 유지해 업스트림 장애 복구를 빠르게 반영한다 (Rev.3). */
    public static final Duration EMPTY_TTL = Duration.ofSeconds(60);
    public static final long MAX_SIZE = 200L;

    private GlobalIndicatorCacheConfig() {}
}
