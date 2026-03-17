package com.thlee.stock.market.stockmarket.economics.application;

import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicator;
import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorCategory;
import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorLatest;
import com.thlee.stock.market.stockmarket.economics.domain.model.EcosKeyStatResult;
import com.thlee.stock.market.stockmarket.economics.domain.model.KeyStatIndicator;
import com.thlee.stock.market.stockmarket.economics.domain.repository.EcosIndicatorLatestRepository;
import com.thlee.stock.market.stockmarket.economics.domain.repository.EcosIndicatorRepository;
import com.thlee.stock.market.stockmarket.economics.domain.service.EcosIndicatorPort;
import com.thlee.stock.market.stockmarket.economics.infrastructure.korea.ecos.config.EcosCacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EcosIndicatorSaveService {

    private final EcosIndicatorPort ecosIndicatorPort;
    private final EcosIndicatorRepository ecosIndicatorRepository;
    private final EcosIndicatorLatestRepository ecosIndicatorLatestRepository;
    private final CacheManager ecosCacheManager;

    /**
     * API 1회 조회 → latest 벌크 조회 (1회) → Java 비교 → 변경분 히스토리 INSERT + 캐시 적재 + latest UPSERT
     *
     * @return 저장된 지표 수
     */
    @Transactional
    public int fetchAndSave() {
        try {
            LocalDate today = LocalDate.now();

            // 1. API 조회 (1회)
            EcosKeyStatResult result = ecosIndicatorPort.fetchKeyStatistics();

            // 2. 유효 지표 필터링
            List<KeyStatIndicator> validIndicators = result.validIndicators();

            // 3. 히스토리 존재 여부 확인
            boolean historyExists = ecosIndicatorRepository.existsAny();

            if (!historyExists) {
                // 초기 시딩: 전체 저장
                int count = initialSeed(validIndicators, today);
                putCacheByCategory(result.indicators(), Map.of());
                return count;
            }

            // 기존 로직: cycle 비교 → 변경분만 저장 + 캐시 적재
            return saveChangedIndicators(result.indicators(), validIndicators, today);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 초기 시딩: 히스토리가 비어있을 때 전체 지표 저장
     */
    private int initialSeed(List<KeyStatIndicator> validIndicators, LocalDate today) {
        List<EcosIndicator> allIndicators = validIndicators.stream()
                .map(indicator -> EcosIndicator.fromKeyStatIndicator(indicator, today))
                .toList();

        ecosIndicatorRepository.saveAll(allIndicators);

        List<EcosIndicatorLatest> latestList = validIndicators.stream()
                .map(EcosIndicatorLatest::fromKeyStatIndicator)
                .toList();
        ecosIndicatorLatestRepository.saveAll(latestList);

        log.info("ECOS 초기 시딩 완료: date={}, count={}", today, allIndicators.size());
        return allIndicators.size();
    }

    /**
     * latest cycle 비교 → 변경분만 히스토리 저장 + previousDataValue 보존 + 캐시 적재
     */
    private int saveChangedIndicators(List<KeyStatIndicator> allIndicators,
                                       List<KeyStatIndicator> validIndicators,
                                       LocalDate today) {
        // latest 전체 조회 (1회) → Map<compareKey, EcosIndicatorLatest>
        Map<String, EcosIndicatorLatest> latestMap = new HashMap<>();
        for (EcosIndicatorLatest latest : ecosIndicatorLatestRepository.findAll()) {
            latestMap.put(latest.toCompareKey(), latest);
        }

        // 변경분 추출
        List<EcosIndicator> changedIndicators = validIndicators.stream()
                .filter(indicator -> isCycleChanged(indicator, latestMap))
                .map(indicator -> EcosIndicator.fromKeyStatIndicator(indicator, today))
                .toList();

        if (!changedIndicators.isEmpty()) {
            ecosIndicatorRepository.saveAll(changedIndicators);
            log.info("ECOS 히스토리 저장 완료: date={}, count={}", today, changedIndicators.size());
        } else {
            log.info("ECOS 지표 변경 없음, 히스토리 저장 스킵");
        }

        // latest 전체 갱신 — dataValue + previousDataValue 보존
        List<EcosIndicatorLatest> latestList = validIndicators.stream()
                .map(indicator -> {
                    EcosIndicatorLatest existing = latestMap.get(indicator.toCompareKey());
                    boolean cycleChanged = existing != null
                        && indicator.cycle() != null
                        && !indicator.cycle().equals(existing.getCycle());

                    String previousDataValue = existing != null
                        ? (cycleChanged ? existing.getDataValue() : existing.getPreviousDataValue())
                        : null;

                    return new EcosIndicatorLatest(
                        indicator.className(),
                        indicator.keystatName(),
                        indicator.dataValue(),
                        previousDataValue,
                        indicator.cycle(),
                        LocalDateTime.now()
                    );
                })
                .toList();
        ecosIndicatorLatestRepository.saveAll(latestList);

        // 캐시 적재 (previousDataValue pre-merge)
        putCacheByCategory(allIndicators, latestMap);

        return changedIndicators.size();
    }

    /**
     * 전체 지표를 카테고리별로 분류하여 15개 캐시 키에 적재 (previousDataValue pre-merge)
     */
    private void putCacheByCategory(List<KeyStatIndicator> indicators,
                                     Map<String, EcosIndicatorLatest> latestMap) {
        Cache cache = ecosCacheManager.getCache(EcosCacheConfig.ECOS_INDICATOR_CACHE);
        if (cache == null) {
            log.warn("ECOS 캐시를 찾을 수 없음: {}", EcosCacheConfig.ECOS_INDICATOR_CACHE);
            return;
        }

        // previousDataValue 병합
        List<KeyStatIndicator> enriched = indicators.stream()
            .map(ind -> {
                EcosIndicatorLatest latest = latestMap.get(ind.toCompareKey());
                String prevValue = latest != null ? latest.getPreviousDataValue() : null;
                return ind.withPreviousDataValue(prevValue);
            })
            .toList();

        Map<EcosIndicatorCategory, List<KeyStatIndicator>> grouped = enriched.stream()
            .filter(indicator -> EcosIndicatorCategory.fromClassName(indicator.className()) != null)
            .collect(Collectors.groupingBy(
                indicator -> EcosIndicatorCategory.fromClassName(indicator.className())
            ));

        for (EcosIndicatorCategory category : EcosIndicatorCategory.values()) {
            List<KeyStatIndicator> categoryIndicators = grouped.getOrDefault(category, List.of());
            cache.put(category.name(), categoryIndicators);
        }

        log.info("ECOS 캐시 적재 완료: {}개 카테고리", grouped.size());
    }

    /**
     * latest Map과 비교하여 통계 기준 시점(cycle) 변경 여부 판단
     */
    private boolean isCycleChanged(KeyStatIndicator indicator, Map<String, EcosIndicatorLatest> latestMap) {
        String apiCycle = indicator.cycle();
        EcosIndicatorLatest latest = latestMap.get(indicator.toCompareKey());

        // API 응답 cycle이 null → 데이터 미제공 지표, 저장 불필요
        if (apiCycle == null) {
            return false;
        }

        // latest에 없으면 신규 → 저장 대상
        // cycle이 다르면 → 저장 대상
        return latest == null || !apiCycle.equals(latest.getCycle());
    }
}
