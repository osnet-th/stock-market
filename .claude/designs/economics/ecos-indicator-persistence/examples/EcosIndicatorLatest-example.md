# EcosIndicatorLatest 도메인 모델 구현 예시

```java
package com.thlee.stock.market.stockmarket.economics.domain.model;

import java.time.LocalDateTime;

/**
 * ECOS 경제지표 최신값 도메인 모델 (변경 감지 비교용)
 */
public class EcosIndicatorLatest {

    private final String className;
    private final String keystatName;
    private final String cycle;
    private final LocalDateTime updatedAt;

    public EcosIndicatorLatest(String className,
                                String keystatName,
                                String cycle,
                                LocalDateTime updatedAt) {
        this.className = className;
        this.keystatName = keystatName;
        this.cycle = cycle;
        this.updatedAt = updatedAt;
    }

    /**
     * KeyStatIndicator로부터 최신값 생성
     */
    public static EcosIndicatorLatest fromKeyStatIndicator(KeyStatIndicator indicator) {
        return new EcosIndicatorLatest(
            indicator.className(),
            indicator.keystatName(),
            indicator.cycle(),
            LocalDateTime.now()
        );
    }

    /**
     * 비교 키 생성 (className + keystatName)
     */
    public String toCompareKey() {
        return className + "::" + keystatName;
    }

    public String getClassName() { return className; }
    public String getKeystatName() { return keystatName; }
    public String getCycle() { return cycle; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
```