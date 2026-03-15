package com.thlee.stock.market.stockmarket.stock.domain.service;

import java.math.BigDecimal;

/**
 * 환율 조회 포트.
 * 통화코드에 대한 원화 환율 (1 외화 = N원)을 반환한다.
 */
public interface ExchangeRatePort {
    BigDecimal getRate(String currency);
}