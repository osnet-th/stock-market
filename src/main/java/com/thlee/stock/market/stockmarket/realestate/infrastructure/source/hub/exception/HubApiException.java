package com.thlee.stock.market.stockmarket.realestate.infrastructure.source.hub.exception;

import com.thlee.stock.market.stockmarket.realestate.infrastructure.exception.RealEstateMarketApiException;

public class HubApiException extends RealEstateMarketApiException {
    public HubApiException(String message) { super(message); }
    public HubApiException(String message, Throwable cause) { super(message, cause); }
}
