package com.thlee.stock.market.stockmarket.realestate.infrastructure.source.subscriptionhome.exception;

import com.thlee.stock.market.stockmarket.realestate.infrastructure.exception.RealEstateMarketApiException;

public class SubscriptionHomeApiException extends RealEstateMarketApiException {
    public SubscriptionHomeApiException(String message) { super(message); }
    public SubscriptionHomeApiException(String message, Throwable cause) { super(message, cause); }
}