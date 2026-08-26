package com.thlee.stock.market.stockmarket.realestate.infrastructure.exception;

/**
 * 부동산 출처 어댑터 호출 실패. 공공포털 에러 코드, 인증 실패, 네트워크 오류를 한 예외로 흡수한다.
 */
public class RealEstateMarketApiException extends RuntimeException {

    public RealEstateMarketApiException(String message) {
        super(message);
    }

    public RealEstateMarketApiException(String message, Throwable cause) {
        super(message, cause);
    }
}