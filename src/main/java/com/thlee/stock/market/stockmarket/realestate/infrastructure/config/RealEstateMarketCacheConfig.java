package com.thlee.stock.market.stockmarket.realestate.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Collection;

/**
 * realestate 도메인 전용 캐시 설정.
 * <p>
 * named bean({@code realEstateCacheManager}) — ECOS의 @Primary CacheManager와 충돌하지 않게 분리.
 * 정상 응답은 25h TTL(일배치 + 1h 여유), 빈 응답은 60s로 짧게 유지하여 외부 장애 복구를 빠르게 반영.
 */
@Configuration
public class RealEstateMarketCacheConfig {

    public static final String CACHE_BEAN_NAME = "realEstateCacheManager";
    public static final String INDICATOR_CACHE = "realEstateIndicators";
    public static final String SOURCE_META_CACHE = "realEstateSourceMeta";

    public static final Duration TTL = Duration.ofHours(25);
    public static final Duration EMPTY_TTL = Duration.ofSeconds(60);
    public static final long MAX_SIZE = 1000L;

    @Bean(name = CACHE_BEAN_NAME)
    public CacheManager realEstateCacheManager() {
        CaffeineCacheManager cacheManager =
                new CaffeineCacheManager(INDICATOR_CACHE, SOURCE_META_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfter(new EmptyAwareExpiry())
                .maximumSize(MAX_SIZE));
        return cacheManager;
    }

    /**
     * 빈 컬렉션은 짧게(60s), 그 외는 25h. 출처 장애로 빈 응답이 캐시에 박혀 사용자에게
     * 장기 노출되는 것을 방지.
     */
    static final class EmptyAwareExpiry implements Expiry<Object, Object> {
        @Override
        public long expireAfterCreate(Object key, Object value, long currentTime) {
            return ttlFor(value).toNanos();
        }

        @Override
        public long expireAfterUpdate(Object key, Object value,
                                      long currentTime, long currentDuration) {
            return ttlFor(value).toNanos();
        }

        @Override
        public long expireAfterRead(Object key, Object value,
                                    long currentTime, long currentDuration) {
            return currentDuration;
        }

        private Duration ttlFor(Object value) {
            if (value instanceof Collection<?> collection && collection.isEmpty()) {
                return EMPTY_TTL;
            }
            return TTL;
        }
    }
}