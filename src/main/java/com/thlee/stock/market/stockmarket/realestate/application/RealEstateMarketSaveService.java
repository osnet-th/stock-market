package com.thlee.stock.market.stockmarket.realestate.application;

import com.thlee.stock.market.stockmarket.realestate.domain.model.IndicatorKey;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketCategory;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketIndicator;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketLatest;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketSource;
import com.thlee.stock.market.stockmarket.realestate.domain.repository.RealEstateMarketIndicatorRepository;
import com.thlee.stock.market.stockmarket.realestate.domain.repository.RealEstateMarketLatestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 어댑터의 fetch 결과를 latest와 비교해 변경분만 history에 적재하고 latest를 upsert.
 * <p>
 * Map lookup 키는 PK 단위 {@link IndicatorKey} (regionCode, category, source, indicatorCode).
 * referenceText는 row 단위 메타이므로 키에서 제외 — 롤오버 시 lookup miss를 방지하고
 * previousReferenceText 보존을 정상화한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealEstateMarketSaveService {

    private final RealEstateMarketIndicatorRepository indicatorRepository;
    private final RealEstateMarketLatestRepository latestRepository;

    /**
     * 단일 (region, category, source)에 대해 fetch 결과를 저장.
     */
    @Transactional
    public int upsert(String regionCode,
                      RealEstateMarketCategory category,
                      RealEstateMarketSource source,
                      List<RealEstateMarketIndicator> fetched) {
        if (fetched == null || fetched.isEmpty()) {
            return 0;
        }

        Map<IndicatorKey, RealEstateMarketLatest> latestMap = loadLatestMap(regionCode, category, source);

        List<RealEstateMarketIndicator> changed = filterChanged(fetched, latestMap);
        if (!changed.isEmpty()) {
            indicatorRepository.saveAll(changed);
            log.info("[realestate.save] history insert: region={}, category={}, source={}, count={}",
                    regionCode, category, source, changed.size());
        }

        List<RealEstateMarketLatest> latests = mergeLatest(fetched, latestMap);
        latestRepository.upsertAll(latests);

        return changed.size();
    }

    private Map<IndicatorKey, RealEstateMarketLatest> loadLatestMap(String regionCode,
                                                                     RealEstateMarketCategory category,
                                                                     RealEstateMarketSource source) {
        Map<IndicatorKey, RealEstateMarketLatest> map = new HashMap<>();
        for (RealEstateMarketLatest latest : latestRepository.findAllByRegionAndCategoryAndSource(
                regionCode, category, source)) {
            map.put(latest.pkKey(), latest);
        }
        return map;
    }

    private List<RealEstateMarketIndicator> filterChanged(List<RealEstateMarketIndicator> fetched,
                                                           Map<IndicatorKey, RealEstateMarketLatest> latestMap) {
        List<RealEstateMarketIndicator> result = new ArrayList<>();
        for (RealEstateMarketIndicator indicator : fetched) {
            RealEstateMarketLatest existing = latestMap.get(indicator.pkKey());
            if (existing == null || !sameValue(existing, indicator)) {
                result.add(indicator);
            }
        }
        return result;
    }

    private boolean sameValue(RealEstateMarketLatest existing, RealEstateMarketIndicator candidate) {
        if (existing.getValue() == null && candidate.getValue() == null) {
            return existing.getReferenceText() != null
                    && existing.getReferenceText().equals(candidate.getReferenceText());
        }
        if (existing.getValue() == null || candidate.getValue() == null) {
            return false;
        }
        return existing.getValue().compareTo(candidate.getValue()) == 0;
    }

    private List<RealEstateMarketLatest> mergeLatest(List<RealEstateMarketIndicator> fetched,
                                                      Map<IndicatorKey, RealEstateMarketLatest> latestMap) {
        LocalDateTime now = LocalDateTime.now();
        List<RealEstateMarketLatest> result = new ArrayList<>(fetched.size());
        for (RealEstateMarketIndicator ind : fetched) {
            RealEstateMarketLatest existing = latestMap.get(ind.pkKey());
            String previousReference = existing == null
                    ? null
                    : (referenceChanged(existing, ind) ? existing.getReferenceText() : existing.getPreviousReferenceText());
            result.add(RealEstateMarketLatest.builder()
                    .regionCode(ind.getRegionCode())
                    .category(ind.getCategory())
                    .source(ind.getSource())
                    .indicatorCode(ind.getIndicatorCode())
                    .referenceText(ind.getReferenceText())
                    .previousReferenceText(previousReference)
                    .value(ind.getValue())
                    .payload(ind.getPayload())
                    .updatedAt(now)
                    .build());
        }
        return result;
    }

    private boolean referenceChanged(RealEstateMarketLatest existing, RealEstateMarketIndicator candidate) {
        if (existing.getReferenceText() == null) {
            return candidate.getReferenceText() != null;
        }
        return !existing.getReferenceText().equals(candidate.getReferenceText());
    }
}