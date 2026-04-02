package com.thlee.stock.market.stockmarket.economics.application;

import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicator;
import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorCategory;
import com.thlee.stock.market.stockmarket.economics.domain.model.EcosKeyStatResult;
import com.thlee.stock.market.stockmarket.economics.domain.model.KeyStatIndicator;
import com.thlee.stock.market.stockmarket.economics.domain.repository.EcosIndicatorRepository;
import com.thlee.stock.market.stockmarket.economics.domain.service.EcosIndicatorPort;
import com.thlee.stock.market.stockmarket.economics.infrastructure.korea.ecos.config.EcosCacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EcosIndicatorService {

    private final EcosIndicatorPort ecosIndicatorPort;
    private final EcosIndicatorRepository ecosIndicatorRepository;
    private final CacheManager ecosCacheManager;

    /**
     * 카테고리별 경제지표 조회
     * 캐시 hit → 즉시 반환, 캐시 miss → API 직접 조회 (배치 실패 fallback)
     */
    @SuppressWarnings("unchecked")
    public List<KeyStatIndicator> getIndicatorsByCategory(EcosIndicatorCategory category) {
        Cache cache = ecosCacheManager.getCache(EcosCacheConfig.ECOS_INDICATOR_CACHE);

        if (cache != null) {
            List<KeyStatIndicator> cached = cache.get(category.name(), List.class);
            if (cached != null) {
                return cached;
            }
        }

        // 캐시 miss: 배치 실패 등 예외 상황 → API 직접 조회
        log.warn("ECOS 캐시 miss, API fallback 조회: category={}", category);
        EcosKeyStatResult result = ecosIndicatorPort.fetchKeyStatistics();

        return result.indicators().stream()
            .filter(indicator -> category.contains(indicator.className()))
            .toList();
    }

    /**
     * 카테고리별 경제지표 히스토리 조회
     * DB에서 cycle별 deduplicate된 데이터를 반환
     */
    @Transactional(readOnly = true)
    public List<EcosIndicator> getHistoryByCategory(EcosIndicatorCategory category) {
        return ecosIndicatorRepository.findLatestHistoryByClassNames(category.getClassNames());
    }
}