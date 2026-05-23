package com.thlee.stock.market.stockmarket.realestate.infrastructure.scheduler;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketLatest;
import com.thlee.stock.market.stockmarket.realestate.domain.repository.RealEstateMarketLatestRepository;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.config.RealEstateMarketCacheConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 부동산 도메인 Warmup. <strong>외부 API를 호출하지 않는다.</strong>
 * <p>
 * DB의 latest를 읽어 region 단위로 그룹핑한 뒤 Caffeine 캐시에 hot-load만 수행.
 * 외부 fetch는 일배치(06:00 KST)에서만 실행되며, 부팅마다 8개 출처 × 56시군 × 9카테고리의
 * 외부 호출이 발생하지 않도록 명시적으로 분리되어 있다.
 */
@Component
@Slf4j
public class RealEstateMarketWarmupListener {

    private final RealEstateMarketLatestRepository latestRepository;
    private final CacheManager realEstateCacheManager;

    public RealEstateMarketWarmupListener(
            RealEstateMarketLatestRepository latestRepository,
            @Qualifier(RealEstateMarketCacheConfig.CACHE_BEAN_NAME) CacheManager realEstateCacheManager) {
        this.latestRepository = latestRepository;
        this.realEstateCacheManager = realEstateCacheManager;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            List<RealEstateMarketLatest> all = latestRepository.findAll();
            if (all.isEmpty()) {
                log.info("[realestate.warmup] latest empty, skip cache hot-load");
                return;
            }

            Cache cache = realEstateCacheManager.getCache(RealEstateMarketCacheConfig.INDICATOR_CACHE);
            if (cache == null) {
                log.warn("[realestate.warmup] cache not found: {}", RealEstateMarketCacheConfig.INDICATOR_CACHE);
                return;
            }

            Map<String, List<RealEstateMarketLatest>> grouped = all.stream()
                    .collect(Collectors.groupingBy(l -> cacheKey(l.getRegionCode(), l.getCategory().name())));

            Map<String, Integer> putCount = new HashMap<>();
            grouped.forEach((key, list) -> {
                cache.put(key, list);
                putCount.put(key, list.size());
            });

            log.info("[realestate.warmup] cache hot-load completed: keys={}, totalRows={}",
                    putCount.size(), all.size());
        } catch (Exception e) {
            log.warn("[realestate.warmup] hot-load failed (non-fatal)", e);
        }
    }

    public static String cacheKey(String regionCode, String categoryName) {
        return regionCode + "::" + categoryName;
    }
}