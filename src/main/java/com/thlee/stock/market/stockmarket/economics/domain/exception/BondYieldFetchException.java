package com.thlee.stock.market.stockmarket.economics.domain.exception;

/**
 * 금리 출처와 통신하지 못함 (연결·타임아웃·HTTP 오류·조회 시간 초과).
 * 데이터 없음(빈 날짜)과 구분되는 실패이며, 출처와 무관하게 이 예외로 알린다.
 */
public class BondYieldFetchException extends RuntimeException {

    public BondYieldFetchException(String message) {
        super(message);
    }

    public BondYieldFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
