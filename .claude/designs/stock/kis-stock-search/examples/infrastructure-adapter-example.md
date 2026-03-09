# KisStockAdapter 구현 예시

```java
package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis;

import com.thlee.stock.market.stockmarket.stock.domain.model.Stock;
import com.thlee.stock.market.stockmarket.stock.domain.service.StockPort;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisMasterStock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class KisStockAdapter implements StockPort {

    private final KisStockMasterCache stockMasterCache;

    @Override
    public List<Stock> searchByName(String stockName) {
        List<KisMasterStock> masterStocks = stockMasterCache.searchByName(stockName);

        return masterStocks.stream()
            .map(this::toDomainModel)
            .toList();
    }

    private Stock toDomainModel(KisMasterStock master) {
        return new Stock(
            master.getStockCode(),
            master.getKoreanName(),
            master.getEnglishName(),
            master.getMarketType(),
            master.getExchangeCode()
        );
    }
}
```