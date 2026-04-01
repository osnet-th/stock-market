package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.sec.exception;

public enum SecErrorType {
    CIK_NOT_FOUND,
    COMPANY_NOT_FOUND,
    RATE_LIMIT_EXCEEDED,
    API_ERROR,
    UNKNOWN
}