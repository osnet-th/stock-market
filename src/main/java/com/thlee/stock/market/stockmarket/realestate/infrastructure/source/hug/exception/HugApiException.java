package com.thlee.stock.market.stockmarket.realestate.infrastructure.source.hug.exception;

import com.thlee.stock.market.stockmarket.realestate.infrastructure.exception.RealEstateMarketApiException;

public class HugApiException extends RealEstateMarketApiException {
    public HugApiException(String message) { super(message); }
    public HugApiException(String message, Throwable cause) { super(message, cause); }
}