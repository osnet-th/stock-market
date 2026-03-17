# Presentation 변경 예시

## EcosIndicatorController 변경

```java
// 변경 전
private final EcosIndicatorMetadataProperties metadataProperties;

Map<String, EcosIndicatorMetadataProperties.IndicatorMeta> metaMap =
    metadataProperties.toMap();

// 변경 후
private final EcosIndicatorMetadataService metadataService;

Map<String, EcosIndicatorMetadata> metaMap =
    metadataService.getMetadataMap();
```

## IndicatorResponse 변경

```java
// 변경 전
public static IndicatorResponse from(KeyStatIndicator indicator,
                                      EcosIndicatorMetadataProperties.IndicatorMeta meta)

// 변경 후
public static IndicatorResponse from(KeyStatIndicator indicator,
                                      EcosIndicatorMetadata meta) {
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
```