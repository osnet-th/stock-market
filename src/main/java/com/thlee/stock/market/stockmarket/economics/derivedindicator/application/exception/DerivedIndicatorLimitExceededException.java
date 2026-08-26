package com.thlee.stock.market.stockmarket.economics.derivedindicator.application.exception;

/**
 * 사용자별 파생지표 개수 상한 초과 → 400. (자원 고갈 방지)
 */
public class DerivedIndicatorLimitExceededException extends RuntimeException {
    public DerivedIndicatorLimitExceededException(int limit) {
        super("파생지표는 최대 " + limit + "개까지 만들 수 있습니다.");
    }
}
