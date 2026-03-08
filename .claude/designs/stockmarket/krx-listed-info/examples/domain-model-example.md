# Domain 계층 코드 예시

## ListedStock (도메인 모델)

```java
package com.thlee.stock.market.stockmarket.stock.domain.model;

/**
 * 상장 종목 정보 (API 출처에 무관한 순수 도메인 모델)
 */
public record ListedStock(
    String stockCode,   // 종목코드 (6자리, 예: "005930")
    String stockName,   // 종목명 (예: "삼성전자")
    String marketType,  // 시장구분 (KOSPI/KOSDAQ/KONEX)
    String corpName     // 법인명 (예: "삼성전자주식회사")
) {
}
```

## ListedStockPort (포트 인터페이스)

```java
package com.thlee.stock.market.stockmarket.stock.domain.service;

import com.thlee.stock.market.stockmarket.stock.domain.model.ListedStock;

import java.util.List;

/**
 * 상장 종목 조회 포트
 */
public interface ListedStockPort {
    List<ListedStock> searchByName(String stockName);
}
```