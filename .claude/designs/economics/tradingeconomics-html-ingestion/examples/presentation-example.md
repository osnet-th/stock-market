# Presentation 구현 예시

## CountryIndicatorRowResponse (presentation/dto)

```java
package com.thlee.stock.market.stockmarket.economics.presentation.dto;

import com.thlee.stock.market.stockmarket.economics.domain.model.CountryIndicatorSnapshot;
import com.thlee.stock.market.stockmarket.economics.domain.model.IndicatorValue;

import java.math.BigDecimal;

public record CountryIndicatorRowResponse(
    String countryName,
    BigDecimal lastValue,
    BigDecimal previousValue,
    String reference,
    String unit
) {

    public static CountryIndicatorRowResponse from(CountryIndicatorSnapshot snapshot) {
        return new CountryIndicatorRowResponse(
            snapshot.getCountryName(),
            toNumeric(snapshot.getLastValue()),
            toNumeric(snapshot.getPreviousValue()),
            snapshot.getReferenceText(),
            toUnit(snapshot.getLastValue())
        );
    }

    private static BigDecimal toNumeric(IndicatorValue value) {
        return value != null ? value.getNumericValue() : null;
    }

    private static String toUnit(IndicatorValue value) {
        return value != null ? value.getUnit() : null;
    }
}
```

## GlobalIndicatorResponse (presentation/dto)

```java
package com.thlee.stock.market.stockmarket.economics.presentation.dto;

import com.thlee.stock.market.stockmarket.economics.domain.model.CountryIndicatorSnapshot;
import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalEconomicIndicatorType;

import java.util.List;

public record GlobalIndicatorResponse(
    String indicatorType,
    String displayName,
    String category,
    int count,
    List<CountryIndicatorRowResponse> countries
) {

    public static GlobalIndicatorResponse of(
            GlobalEconomicIndicatorType type,
            List<CountryIndicatorSnapshot> snapshots) {
        List<CountryIndicatorRowResponse> rows = snapshots.stream()
            .map(CountryIndicatorRowResponse::from)
            .toList();

        return new GlobalIndicatorResponse(
            type.name(),
            type.getDisplayName(),
            type.getCategory().getDisplayName(),
            rows.size(),
            rows
        );
    }
}
```

## IndicatorSummaryResponse (presentation/dto)

```java
package com.thlee.stock.market.stockmarket.economics.presentation.dto;

public record IndicatorSummaryResponse(
    String key,
    String displayName
) {
}
```

## CategoryResponse (presentation/dto)

```java
package com.thlee.stock.market.stockmarket.economics.presentation.dto;

import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalEconomicIndicatorType;
import com.thlee.stock.market.stockmarket.economics.domain.model.IndicatorCategory;

import java.util.Arrays;
import java.util.List;

public record CategoryResponse(
    String key,
    String displayName,
    List<IndicatorSummaryResponse> indicators
) {

    public static List<CategoryResponse> fromAll() {
        return Arrays.stream(IndicatorCategory.values())
            .map(CategoryResponse::from)
            .toList();
    }

    private static CategoryResponse from(IndicatorCategory category) {
        List<IndicatorSummaryResponse> indicators = Arrays.stream(GlobalEconomicIndicatorType.values())
            .filter(type -> type.getCategory() == category)
            .map(type -> new IndicatorSummaryResponse(type.name(), type.getDisplayName()))
            .toList();

        return new CategoryResponse(
            category.name(),
            category.getDisplayName(),
            indicators
        );
    }
}
```

## GlobalCategoryIndicatorResponse (presentation/dto)

```java
package com.thlee.stock.market.stockmarket.economics.presentation.dto;

import com.thlee.stock.market.stockmarket.economics.domain.model.CountryIndicatorSnapshot;
import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalEconomicIndicatorType;
import com.thlee.stock.market.stockmarket.economics.domain.model.IndicatorCategory;

import java.util.List;
import java.util.Map;

/**
 * 카테고리별 전체 지표 데이터 응답 DTO
 */
public record GlobalCategoryIndicatorResponse(
    String categoryKey,
    String categoryDisplayName,
    List<GlobalIndicatorResponse> indicators
) {

    public static GlobalCategoryIndicatorResponse of(
            IndicatorCategory category,
            Map<GlobalEconomicIndicatorType, List<CountryIndicatorSnapshot>> snapshotsByType) {

        List<GlobalIndicatorResponse> indicators = snapshotsByType.entrySet().stream()
            .map(entry -> GlobalIndicatorResponse.of(entry.getKey(), entry.getValue()))
            .toList();

        return new GlobalCategoryIndicatorResponse(
            category.name(),
            category.getDisplayName(),
            indicators
        );
    }
}
```

## GlobalIndicatorController (presentation)

```java
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
import org.springframework.web.bind.annotation.*;

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
```