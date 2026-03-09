# KisStockPriceAdapter 구현 예시

```java
package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis;

import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import com.thlee.stock.market.stockmarket.stock.domain.model.StockPrice;
import com.thlee.stock.market.stockmarket.stock.domain.service.StockPricePort;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisDomesticPriceOutput;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisOverseasPriceOutput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * KIS API를 통한 주식 현재가 조회 어댑터.
 * StockPricePort 구현체.
 */
@Component
@RequiredArgsConstructor
public class KisStockPriceAdapter implements StockPricePort {

    private final KisStockPriceClient priceClient;

    @Override
    public StockPrice getPrice(String stockCode, MarketType marketType, ExchangeCode exchangeCode) {
        if (marketType.isDomestic()) {
            return toDomesticStockPrice(priceClient.getDomesticPrice(stockCode), stockCode, marketType, exchangeCode);
        }
        return toOverseasStockPrice(priceClient.getOverseasPrice(stockCode, exchangeCode), stockCode, marketType, exchangeCode);
    }

    private StockPrice toDomesticStockPrice(KisDomesticPriceOutput output,
                                            String stockCode,
                                            MarketType marketType,
                                            ExchangeCode exchangeCode) {
        return new StockPrice(
            stockCode,
            output.getCurrentPrice(),
            output.getPreviousClose(),
            output.getChange(),
            output.getChangeSign(),
            output.getChangeRate(),
            output.getVolume(),
            output.getTradingAmount(),
            output.getHighPrice(),
            output.getLowPrice(),
            output.getOpenPrice(),
            marketType,
            exchangeCode
        );
    }

    private StockPrice toOverseasStockPrice(KisOverseasPriceOutput output,
                                            String stockCode,
                                            MarketType marketType,
                                            ExchangeCode exchangeCode) {
        return new StockPrice(
            stockCode,
            output.getCurrentPrice(),
            output.getPreviousClose(),
            output.getChange(),
            output.getChangeSign(),
            output.getChangeRate(),
            output.getVolume(),
            output.getTradingAmount(),
            null,   // 해외는 고가 미제공
            null,   // 해외는 저가 미제공
            null,   // 해외는 시가 미제공
            marketType,
            exchangeCode
        );
    }
}
```