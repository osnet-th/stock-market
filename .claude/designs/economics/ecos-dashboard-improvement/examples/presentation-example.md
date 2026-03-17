# 프레젠테이션 예시

## IndicatorResponse DTO (확장)

```java
package com.thlee.stock.market.stockmarket.economics.presentation.dto;

import com.thlee.stock.market.stockmarket.economics.domain.model.KeyStatIndicator;
import com.thlee.stock.market.stockmarket.economics.infrastructure.korea.ecos.config.EcosIndicatorMetadataProperties;

public record IndicatorResponse(
    String className,
    String keystatName,
    String dataValue,
    String previousDataValue,
    String cycle,
    String unitName,
    String description,
    String positiveDirection,
    boolean keyIndicator
) {

    public static IndicatorResponse from(KeyStatIndicator indicator,
                                          EcosIndicatorMetadataProperties.IndicatorMeta meta) {
        return new IndicatorResponse(
            indicator.className(),
            indicator.keystatName(),
            indicator.dataValue(),
            indicator.previousDataValue(),
            indicator.cycle(),
            indicator.unitName(),
            meta != null ? meta.getDescription() : null,
            meta != null ? meta.getPositiveDirection().name() : "NEUTRAL",
            meta != null && meta.isKeyIndicator()
        );
    }
}
```

## EcosIndicatorController (변경)

```java
@GetMapping
public ResponseEntity<List<IndicatorResponse>> getIndicatorsByCategory(
        @RequestParam EcosIndicatorCategory category
) {
    List<KeyStatIndicator> indicators = ecosIndicatorService.getIndicatorsByCategory(category);
    Map<String, EcosIndicatorMetadataProperties.IndicatorMeta> metaMap =
        metadataProperties.getIndicators();

    List<IndicatorResponse> response = indicators.stream()
            .map(ind -> IndicatorResponse.from(ind, metaMap.get(ind.toCompareKey())))
            .toList();

    return ResponseEntity.ok(response);
}
```