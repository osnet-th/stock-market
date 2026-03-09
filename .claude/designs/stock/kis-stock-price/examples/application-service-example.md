# Application 계층 예시

## StockPriceService

```java
package com.thlee.stock.market.stockmarket.stock.application;

import com.thlee.stock.market.stockmarket.stock.application.dto.StockPriceResponse;
import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import com.thlee.stock.market.stockmarket.stock.domain.model.StockPrice;
import com.thlee.stock.market.stockmarket.stock.domain.service.StockPricePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockPriceService {

    private final StockPricePort stockPricePort;

    public StockPriceResponse getPrice(String stockCode, MarketType marketType, ExchangeCode exchangeCode) {
        StockPrice price = stockPricePort.getPrice(stockCode, marketType, exchangeCode);
        return StockPriceResponse.from(price);
    }
}
```

## StockPriceResponse (응답 DTO)

```java
package com.thlee.stock.market.stockmarket.stock.application.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.StockPrice;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class StockPriceResponse {

    private final String stockCode;
    private final String currentPrice;
    private final String previousClose;
    private final String change;
    private final String changeSign;
    private final String changeRate;
    private final String volume;
    private final String tradingAmount;
    private final String high;
    private final String low;
    private final String open;
    private final String marketType;
    private final String exchangeCode;

    public static StockPriceResponse from(StockPrice price) {
        return new StockPriceResponse(
            price.stockCode(),
            price.currentPrice(),
            price.previousClose(),
            price.change(),
            price.changeSign(),
            price.changeRate(),
            price.volume(),
            price.tradingAmount(),
            price.high(),
            price.low(),
            price.open(),
            price.marketType().name(),
            price.exchangeCode().name()
        );
    }
}
```