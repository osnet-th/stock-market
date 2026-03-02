package com.thlee.stock.market.stockmarket.news.infrastructure.infrastructure.common;

public class NewsApiException extends RuntimeException {
    public NewsApiException(String message, Throwable cause) {
        super(message, cause);
    }
}