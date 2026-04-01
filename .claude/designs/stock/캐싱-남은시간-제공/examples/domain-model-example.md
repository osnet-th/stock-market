# CachedStockPrice 도메인 모델

## CachedStockPrice.java

```java
package com.thlee.stock.market.stockmarket.stock.domain.model;

import java.time.Instant;

/**
 * 캐시 메타데이터를 포함한 주식 현재가 래퍼
 */
public record CachedStockPrice(
    StockPrice stockPrice,
    Instant cachedAt
) {

    public static CachedStockPrice of(StockPrice stockPrice, Instant cachedAt) {
        return new CachedStockPrice(stockPrice, cachedAt);
    }

    public static CachedStockPrice now(StockPrice stockPrice) {
        return new CachedStockPrice(stockPrice, Instant.now());
    }
}
```
