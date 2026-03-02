package com.thlee.stock.market.stockmarket.economics.presentation;

import com.thlee.stock.market.stockmarket.economics.application.GlobalIndicatorQueryService;
import com.thlee.stock.market.stockmarket.economics.domain.model.CountryIndicatorSnapshot;
import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalEconomicIndicatorType;
import com.thlee.stock.market.stockmarket.economics.domain.model.IndicatorCategory;
import com.thlee.stock.market.stockmarket.economics.presentation.dto.GlobalCategoryIndicatorResponse;
import com.thlee.stock.market.stockmarket.economics.presentation.dto.GlobalCategoryResponse;
import com.thlee.stock.market.stockmarket.economics.presentation.dto.GlobalIndicatorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}