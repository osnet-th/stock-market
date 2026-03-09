package com.thlee.stock.market.stockmarket.stock.domain.service;

import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import com.thlee.stock.market.stockmarket.stock.domain.model.StockPrice;

/**
 * 주식 현재가 조회 포트
 */
public interface StockPricePort {
    StockPrice getPrice(String stockCode, MarketType marketType, ExchangeCode exchangeCode);
}