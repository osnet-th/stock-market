package com.thlee.stock.market.stockmarket.economics.presentation;

import com.thlee.stock.market.stockmarket.economics.application.EcosIndicatorService;
import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorCategory;
import com.thlee.stock.market.stockmarket.economics.domain.model.KeyStatIndicator;
import com.thlee.stock.market.stockmarket.economics.infrastructure.korea.ecos.config.EcosIndicatorMetadataProperties;
import com.thlee.stock.market.stockmarket.economics.presentation.dto.CategoryResponse;
import com.thlee.stock.market.stockmarket.economics.presentation.dto.IndicatorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * ECOS 경제지표 조회 API
 */
@RestController
@RequestMapping("/api/economics/indicators")
@RequiredArgsConstructor
public class EcosIndicatorController {

    private final EcosIndicatorService ecosIndicatorService;
    private final EcosIndicatorMetadataProperties metadataProperties;

    /**
     * 카테고리별 경제지표 조회
     *
     * @param category 카테고리 (예: INTEREST_RATE, PRICE, EXCHANGE_RATE)
     * @return 해당 카테고리의 경제지표 목록 (메타데이터 포함)
     */
    @GetMapping
    public ResponseEntity<List<IndicatorResponse>> getIndicatorsByCategory(
            @RequestParam EcosIndicatorCategory category
    ) {
        List<KeyStatIndicator> indicators = ecosIndicatorService.getIndicatorsByCategory(category);
        Map<String, EcosIndicatorMetadataProperties.IndicatorMeta> metaMap =
            metadataProperties.toMap();

        List<IndicatorResponse> response = indicators.stream()
                .map(ind -> IndicatorResponse.from(ind, metaMap.get(ind.toCompareKey())))
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * 전체 카테고리 목록 조회
     *
     * @return 카테고리 name + label 목록
     */
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> getCategories() {
        List<CategoryResponse> response = Arrays.stream(EcosIndicatorCategory.values())
                .map(CategoryResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }
}