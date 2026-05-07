package com.thlee.stock.market.stockmarket.realestate.infrastructure.source.seoulopendata.exception;

import com.thlee.stock.market.stockmarket.realestate.infrastructure.exception.RealEstateMarketApiException;

public class SeoulOpenDataApiException extends RealEstateMarketApiException {
    public SeoulOpenDataApiException(String message) { super(message); }
    public SeoulOpenDataApiException(String message, Throwable cause) { super(message, cause); }
}