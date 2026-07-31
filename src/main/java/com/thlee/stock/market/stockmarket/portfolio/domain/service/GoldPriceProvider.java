package com.thlee.stock.market.stockmarket.portfolio.domain.service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 금 1g당 현재가(KRW) 조회 포트.
 * 조회 실패 시 empty를 반환하고 예외를 전파하지 않는다 (원금 평가 fallback).
 */
public interface GoldPriceProvider {

    Optional<BigDecimal> getPricePerGram();
}
