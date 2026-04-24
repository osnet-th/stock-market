package com.thlee.stock.market.stockmarket.stock.domain.service;

import com.thlee.stock.market.stockmarket.stock.domain.model.CachedStockPrice;
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
     * 일봉(Daily) 가격 히스토리 조회. stocknote 종목 차트 등에서 사용.
     *
     * <p>기본 구현은 빈 리스트 (포트 확장만 추가, 실제 어댑터 구현은 후속 작업). 구현체가
     * 제공하는 경우 {@code date ASC} 정렬로 반환해야 한다.
     *
     * @param from 포함 시작일
     * @param to 포함 종료일
     */
    default List<DailyPrice> getDailyHistory(String stockCode, MarketType marketType, ExchangeCode exchangeCode,
                                             LocalDate from, LocalDate to) {
        return List.of();
    }
}