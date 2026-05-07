package com.thlee.stock.market.stockmarket.realestate.infrastructure.source.ggdatadream.exception;

import com.thlee.stock.market.stockmarket.realestate.infrastructure.exception.RealEstateMarketApiException;

public class GgDataDreamApiException extends RealEstateMarketApiException {
    public GgDataDreamApiException(String message) { super(message); }
    public GgDataDreamApiException(String message, Throwable cause) { super(message, cause); }
}