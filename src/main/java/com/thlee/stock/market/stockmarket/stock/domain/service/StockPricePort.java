package com.thlee.stock.market.stockmarket.stock.domain.service;

import com.thlee.stock.market.stockmarket.stock.domain.model.CachedStockPrice;
import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import com.thlee.stock.market.stockmarket.stock.domain.model.StockPrice;

import java.util.List;
import java.util.Map;

/**
 * 주식 현재가 조회 포트
 */
public interface StockPricePort {
    StockPrice getPrice(String stockCode, MarketType marketType, ExchangeCode exchangeCode);

    /**
     * 국내 주식 멀티종목 시세 일괄조회.
     * 최대 30종목씩 분할하여 KIS 멀티종목 API를 호출한다.
     *
     * @param stockCodes 국내 종목코드 목록
     * @return 종목코드 → StockPrice 매핑
     */
    Map<String, StockPrice> getDomesticPrices(List<String> stockCodes);

    CachedStockPrice getPriceWithCacheInfo(String stockCode, MarketType marketType, ExchangeCode exchangeCode);

    Map<String, CachedStockPrice> getDomesticPricesWithCacheInfo(List<String> stockCodes);
}