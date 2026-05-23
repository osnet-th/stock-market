package com.thlee.stock.market.stockmarket.realestate.infrastructure.source.kosis.exception;

import com.thlee.stock.market.stockmarket.realestate.infrastructure.exception.RealEstateMarketApiException;

public class KosisApiException extends RealEstateMarketApiException {
    public KosisApiException(String message) { super(message); }
    public KosisApiException(String message, Throwable cause) { super(message, cause); }
}