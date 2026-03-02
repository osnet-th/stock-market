package com.thlee.stock.market.stockmarket.economics.application;

import com.thlee.stock.market.stockmarket.economics.domain.model.CountryIndicatorSnapshot;
import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalEconomicIndicatorType;
import com.thlee.stock.market.stockmarket.economics.domain.model.IndicatorCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GlobalIndicatorQueryService {

    private final GlobalIndicatorCacheService globalIndicatorCacheService;

    public List<CountryIndicatorSnapshot> getIndicator(GlobalEconomicIndicatorType indicatorType) {
        return globalIndicatorCacheService.getIndicator(indicatorType);
    }

    /**
     * 카테고리에 속한 모든 지표 데이터를 조회한다.
     * CacheService.getIndicator()를 외부 호출하므로 @Cacheable 프록시가 정상 동작한다.
     */
    public Map<GlobalEconomicIndicatorType, List<CountryIndicatorSnapshot>> getIndicatorsByCategory(
            IndicatorCategory category) {
        return Arrays.stream(GlobalEconomicIndicatorType.values())
            .filter(type -> type.getCategory() == category)
            .collect(Collectors.toMap(
                Function.identity(),
                globalIndicatorCacheService::getIndicator
            ));
    }

    public void evict(GlobalEconomicIndicatorType indicatorType) {
        globalIndicatorCacheService.evict(indicatorType);
    }

    public void evictAll() {
        globalIndicatorCacheService.evictAll();
    }
}