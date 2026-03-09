package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis;

import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import com.thlee.stock.market.stockmarket.stock.domain.model.StockPrice;
import com.thlee.stock.market.stockmarket.stock.domain.service.StockPricePort;
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
            return KisStockPriceMapper.fromDomestic(priceClient.getDomesticPrice(stockCode), stockCode, marketType, exchangeCode);
        }
        return KisStockPriceMapper.fromOverseas(priceClient.getOverseasPrice(stockCode, exchangeCode), stockCode, marketType, exchangeCode);
    }
}