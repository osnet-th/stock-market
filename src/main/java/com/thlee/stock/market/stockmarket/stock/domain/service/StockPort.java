package com.thlee.stock.market.stockmarket.stock.domain.service;

import com.thlee.stock.market.stockmarket.stock.domain.model.Stock;

import java.util.List;
import java.util.Optional;

/**
 * 상장 종목 조회 포트
 */
public interface StockPort {
    List<Stock> searchByName(String stockName);

    Optional<Stock> findByCode(String stockCode);
}