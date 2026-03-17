# 도메인 모델 예시

## KeyStatIndicator (변경)

```java
package com.thlee.stock.market.stockmarket.economics.domain.model;

/**
 * 경제지표 단건 도메인 모델
 */
public record KeyStatIndicator(
    String className,
    String keystatName,
    String dataValue,
    String previousDataValue,  // nullable, 캐시 적재 시 병합
    String cycle,
    String unitName
) {

    /**
     * 비교 키 생성 (className + keystatName)
     */
    public String toCompareKey() {
        return className + "::" + keystatName;
    }

    /**
     * previousDataValue를 병합한 새 인스턴스 생성
     */
    public KeyStatIndicator withPreviousDataValue(String previousDataValue) {
        return new KeyStatIndicator(
            className, keystatName, dataValue, previousDataValue, cycle, unitName
        );
    }
}
```

## EcosIndicatorLatest (변경)

```java
package com.thlee.stock.market.stockmarket.economics.domain.model;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * ECOS 경제지표 최신값 도메인 모델 (변경 감지 + 이전값 보존)
 */
@Getter
public class EcosIndicatorLatest {

    private final String className;
    private final String keystatName;
    private final String dataValue;
    private final String previousDataValue;
    private final String cycle;
    private final LocalDateTime updatedAt;

    public EcosIndicatorLatest(String className,
                                String keystatName,
                                String dataValue,
                                String previousDataValue,
                                String cycle,
                                LocalDateTime updatedAt) {
        this.className = className;
        this.keystatName = keystatName;
        this.dataValue = dataValue;
        this.previousDataValue = previousDataValue;
        this.cycle = cycle;
        this.updatedAt = updatedAt;
    }

    /**
     * KeyStatIndicator로부터 최신값 생성 (초기 시딩용: previousDataValue = null)
     */
    public static EcosIndicatorLatest fromKeyStatIndicator(KeyStatIndicator indicator) {
        return new EcosIndicatorLatest(
            indicator.className(),
            indicator.keystatName(),
            indicator.dataValue(),
            null,
            indicator.cycle(),
            LocalDateTime.now()
        );
    }

    /**
     * 비교 키 생성
     */
    public String toCompareKey() {
        return className + "::" + keystatName;
    }
}
```