package com.thlee.stock.market.stockmarket.economics.domain.exception;

/**
 * 금리 출처 응답을 해석하지 못함 (형식 변경·대상 행 없음·중복·숫자 아님).
 * 데이터 없음(빈 날짜)과 구분되는 실패이며, 출처와 무관하게 이 예외로 알린다.
 */
public class BondYieldParseException extends RuntimeException {

    public BondYieldParseException(String message) {
        super(message);
    }

    public BondYieldParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
