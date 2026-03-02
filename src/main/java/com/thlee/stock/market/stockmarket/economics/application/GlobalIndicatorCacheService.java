package com.thlee.stock.market.stockmarket.economics.application;

import com.thlee.stock.market.stockmarket.economics.domain.model.CountryIndicatorSnapshot;
import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalEconomicIndicatorType;
import com.thlee.stock.market.stockmarket.economics.domain.service.GlobalIndicatorPort;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GlobalIndicatorCacheService {

    private final GlobalIndicatorPort globalIndicatorPort;

    @Cacheable(
        cacheNames = "globalIndicators",
        key = "#indicatorType.name()",
        cacheManager = "globalIndicatorCacheManager",
        unless = "#result == null || #result.isEmpty()"
    )
    public List<CountryIndicatorSnapshot> getIndicator(GlobalEconomicIndicatorType indicatorType) {
        return globalIndicatorPort.fetchByIndicator(indicatorType);
    }

    @CacheEvict(cacheNames = "globalIndicators", key = "#indicatorType.name()", cacheManager = "globalIndicatorCacheManager")
    public void evict(GlobalEconomicIndicatorType indicatorType) {
        // 특정 지표 캐시 무효화
    }

    @CacheEvict(cacheNames = "globalIndicators", allEntries = true, cacheManager = "globalIndicatorCacheManager")
    public void evictAll() {
        // 전체 캐시 초기화
    }
}