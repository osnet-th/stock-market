package com.thlee.stock.market.stockmarket.stock.domain.service;

import com.thlee.stock.market.stockmarket.stock.domain.model.CachedStockPrice;
import com.thlee.stock.market.stockmarket.stock.domain.model.ChartPeriod;
import com.thlee.stock.market.stockmarket.stock.domain.model.DailyPrice;
import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import com.thlee.stock.market.stockmarket.stock.domain.model.StockPrice;

import java.time.LocalDate;
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

    /**
     * 기간별(일/주/월봉) 가격 히스토리 조회.
     *
     * <p>기본 구현은 빈 리스트. 구현체가 제공하는 경우 {@code date ASC} 정렬로 반환해야 한다.
     *
     * @param period 봉 주기 (일/주/월)
     * @param from   포함 시작일
     * @param to     포함 종료일
     */
    default List<DailyPrice> getPriceHistory(String stockCode, MarketType marketType, ExchangeCode exchangeCode,
                                             ChartPeriod period, LocalDate from, LocalDate to) {
        return List.of();
    }
}