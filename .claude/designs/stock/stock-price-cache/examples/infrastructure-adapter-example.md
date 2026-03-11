# KisStockPriceAdapter 구현 예시

```java
package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis;

import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import com.thlee.stock.market.stockmarket.stock.domain.model.StockPrice;
import com.thlee.stock.market.stockmarket.stock.domain.service.StockPricePort;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KisStockPriceAdapter implements StockPricePort {

    private final KisStockPriceClient priceClient;

    @Cacheable(cacheManager = "stockPriceCacheManager", cacheNames = "stockPrice", key = "#stockCode + '_' + #exchangeCode")
    @Override
    public StockPrice getPrice(String stockCode, MarketType marketType, ExchangeCode exchangeCode) {
        if (marketType.isDomestic()) {
            return KisStockPriceMapper.fromDomestic(priceClient.getDomesticPrice(stockCode), stockCode, marketType, exchangeCode);
        }
        return KisStockPriceMapper.fromOverseas(priceClient.getOverseasPrice(stockCode, exchangeCode), stockCode, marketType, exchangeCode);
    }
}
```