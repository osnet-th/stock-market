# EcosIndicatorRepository 도메인 인터페이스 구현 예시

```java
package com.thlee.stock.market.stockmarket.economics.domain.repository;

import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicator;

import java.util.List;

/**
 * ECOS 경제지표 히스토리 저장소
 */
public interface EcosIndicatorRepository {

    /**
     * 경제지표 목록 일괄 저장
     */
    List<EcosIndicator> saveAll(List<EcosIndicator> indicators);
}
```