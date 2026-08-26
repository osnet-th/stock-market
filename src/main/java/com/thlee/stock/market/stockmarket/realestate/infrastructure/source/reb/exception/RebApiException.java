package com.thlee.stock.market.stockmarket.realestate.infrastructure.source.reb.exception;

import com.thlee.stock.market.stockmarket.realestate.infrastructure.exception.RealEstateMarketApiException;

public class RebApiException extends RealEstateMarketApiException {
    public RebApiException(String message) { super(message); }
    public RebApiException(String message, Throwable cause) { super(message, cause); }
}
