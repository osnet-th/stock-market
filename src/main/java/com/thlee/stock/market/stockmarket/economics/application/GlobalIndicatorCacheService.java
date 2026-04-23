package com.thlee.stock.market.stockmarket.economics.application;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.thlee.stock.market.stockmarket.economics.domain.model.CountryIndicatorSnapshot;
import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalEconomicIndicatorType;
import com.thlee.stock.market.stockmarket.economics.domain.service.GlobalIndicatorPort;
import com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics.config.GlobalIndicatorCacheConfig;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Caffeine {@link LoadingCache} 기반 글로벌 지표 캐시 서비스.
 * 동일 key 에 대한 로더 중복 실행은 Caffeine 내장 single-flight 로 억제된다 (Rev.2).
 * 빈 결과는 짧은 TTL (negative cache, 60s) 로 캐싱해 업스트림 장애 시 재시도 폭주를 막는다 (Rev.3).
 */
@Service
public class GlobalIndicatorCacheService {

    private final GlobalIndicatorPort globalIndicatorPort;
    private final LoadingCache<GlobalEconomicIndicatorType, List<CountryIndicatorSnapshot>> cache;

    public GlobalIndicatorCacheService(GlobalIndicatorPort globalIndicatorPort) {
        this.globalIndicatorPort = globalIndicatorPort;
        this.cache = Caffeine.newBuilder()
            .expireAfter(new EmptyAwareExpiry())
            .maximumSize(GlobalIndicatorCacheConfig.MAX_SIZE)
            .build(this::loadFromPort);
    }

    public List<CountryIndicatorSnapshot> getIndicator(GlobalEconomicIndicatorType indicatorType) {
        List<CountryIndicatorSnapshot> result = cache.get(indicatorType);
        return result == null ? List.of() : result;
    }

    public void evict(GlobalEconomicIndicatorType indicatorType) {
        cache.invalidate(indicatorType);
    }

    public void evictAll() {
        cache.invalidateAll();
    }

    /**
     * 사용자가 재조회 버튼을 클릭한 경우에만 호출된다.
     * 캐시를 우회해 실시간으로 재조회한 뒤 성공 시 캐시를 원자적으로 덮어쓴다.
     * 실패 시 예외를 전파하며 기존 캐시는 유지한다.
     */
    public List<CountryIndicatorSnapshot> forceRefresh(GlobalEconomicIndicatorType indicatorType) {
        List<CountryIndicatorSnapshot> fresh = globalIndicatorPort.fetchByIndicator(indicatorType);
        if (fresh != null && !fresh.isEmpty()) {
            cache.put(indicatorType, fresh);
        }
        return fresh;
    }

    /** 로더: port 호출 결과를 그대로 반환(빈 결과 포함). TTL 차등은 {@link EmptyAwareExpiry} 가 담당. */
    private List<CountryIndicatorSnapshot> loadFromPort(GlobalEconomicIndicatorType indicatorType) {
        List<CountryIndicatorSnapshot> loaded = globalIndicatorPort.fetchByIndicator(indicatorType);
        return loaded == null ? List.of() : loaded;
    }

    /** 비어있는 결과는 60s, 그 외는 12h 동안 캐시에 유지한다 (Rev.3). */
    private static final class EmptyAwareExpiry implements Expiry<GlobalEconomicIndicatorType, List<CountryIndicatorSnapshot>> {
        @Override
        public long expireAfterCreate(GlobalEconomicIndicatorType key,
                                      List<CountryIndicatorSnapshot> value,
                                      long currentTime) {
            return ttlFor(value).toNanos();
        }

        @Override
        public long expireAfterUpdate(GlobalEconomicIndicatorType key,
                                      List<CountryIndicatorSnapshot> value,
                                      long currentTime,
                                      long currentDuration) {
            return ttlFor(value).toNanos();
        }

        @Override
        public long expireAfterRead(GlobalEconomicIndicatorType key,
                                    List<CountryIndicatorSnapshot> value,
                                    long currentTime,
                                    long currentDuration) {
            return currentDuration;
        }

        private static java.time.Duration ttlFor(List<CountryIndicatorSnapshot> value) {
            return (value == null || value.isEmpty())
                ? GlobalIndicatorCacheConfig.EMPTY_TTL
                : GlobalIndicatorCacheConfig.TTL;
        }
    }
}