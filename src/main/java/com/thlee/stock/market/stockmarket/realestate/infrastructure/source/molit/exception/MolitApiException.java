package com.thlee.stock.market.stockmarket.realestate.infrastructure.source.molit.exception;

import com.thlee.stock.market.stockmarket.realestate.infrastructure.exception.RealEstateMarketApiException;

/**
 * 국토교통부 data.go.kr 어댑터 호출 실패.
 */
public class MolitApiException extends RealEstateMarketApiException {

    public MolitApiException(String message) {
        super(message);
    }

    public MolitApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
