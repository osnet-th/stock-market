package com.thlee.stock.market.stockmarket.economics.presentation;

import com.thlee.stock.market.stockmarket.economics.application.GlobalIndicatorQueryService;
import com.thlee.stock.market.stockmarket.economics.domain.model.CountryIndicatorSnapshot;
import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalEconomicIndicatorType;
import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalIndicator;
import com.thlee.stock.market.stockmarket.economics.domain.model.IndicatorCategory;
import com.thlee.stock.market.stockmarket.economics.presentation.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/economics/global-indicators")
@RequiredArgsConstructor
public class GlobalIndicatorController {

    private final GlobalIndicatorQueryService globalIndicatorQueryService;

    /**
     * 지표별 G20 국가 데이터 조회
     *
     * GET /api/economics/global-indicators/{indicatorType}
     * 예: GET /api/economics/global-indicators/CORE_CONSUMER_PRICES
     */
    @GetMapping("/{indicatorType}")
    public ResponseEntity<GlobalIndicatorResponse> getIndicator(
            @PathVariable GlobalEconomicIndicatorType indicatorType) {

        List<CountryIndicatorSnapshot> snapshots =
            globalIndicatorQueryService.getIndicator(indicatorType);

        return ResponseEntity.ok(GlobalIndicatorResponse.of(indicatorType, snapshots));
    }

    /**
     * 카테고리별 지표 목록 조회
     *
     * GET /api/economics/global-indicators/categories
     */
    @GetMapping("/categories")
    public ResponseEntity<List<GlobalCategoryResponse>> getCategories() {
        return ResponseEntity.ok(GlobalCategoryResponse.fromAll());
    }

    /**
     * 카테고리에 속한 모든 지표 데이터 조회
     *
     * GET /api/economics/global-indicators/categories/{category}
     * 예: GET /api/economics/global-indicators/categories/TRADE_GDP
     */
    @GetMapping("/categories/{category}")
    public ResponseEntity<GlobalCategoryIndicatorResponse> getIndicatorsByCategory(
            @PathVariable IndicatorCategory category) {

        Map<GlobalEconomicIndicatorType, List<CountryIndicatorSnapshot>> snapshotsByType =
            globalIndicatorQueryService.getIndicatorsByCategory(category);

        return ResponseEntity.ok(GlobalCategoryIndicatorResponse.of(category, snapshotsByType));
    }

    /**
     * 지표타입별 히스토리 조회
     * cycle별 deduplicate된 데이터를 국가별로 그룹핑하여 반환
     *
     * GET /api/economics/global-indicators/{indicatorType}/history
     */
    @GetMapping("/{indicatorType}/history")
    public ResponseEntity<GlobalIndicatorHistoryResponse> getHistoryByIndicatorType(
            @PathVariable GlobalEconomicIndicatorType indicatorType) {

        List<GlobalIndicator> history = globalIndicatorQueryService.getHistoryByIndicatorType(indicatorType);

        // countryName 기준으로 그룹핑 (삽입 순서 유지)
        Map<String, List<GlobalIndicator>> grouped = new LinkedHashMap<>();
        for (GlobalIndicator indicator : history) {
            grouped.computeIfAbsent(indicator.getCountryName(), k -> new ArrayList<>()).add(indicator);
        }

        // 단위는 첫 번째 데이터에서 추출
        String unit = history.isEmpty() ? "" : history.getFirst().getUnit();

        List<CountryHistory> countries = grouped.entrySet().stream()
                .map(entry -> {
                    List<HistoryPoint> points = entry.getValue().stream()
                            .map(ind -> new HistoryPoint(ind.getCycle(), ind.getDataValue()))
                            .toList();
                    return new CountryHistory(entry.getKey(), points);
                })
                .toList();

        GlobalIndicatorHistoryResponse response = new GlobalIndicatorHistoryResponse(
                indicatorType.name(),
                indicatorType.getDisplayName(),
                unit,
                countries
        );

        return ResponseEntity.ok(response);
    }
}