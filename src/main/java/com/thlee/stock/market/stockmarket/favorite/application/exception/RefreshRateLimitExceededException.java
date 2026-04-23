package com.thlee.stock.market.stockmarket.favorite.application.exception;

/**
 * 관심 지표 재조회 레이트 리밋 초과.
 * user+indicatorType 기준 최근 60초 내 2회 이상 시도한 경우 발생한다.
 */
public class RefreshRateLimitExceededException extends RuntimeException {
    public RefreshRateLimitExceededException(String message) {
        super(message);
    }
}