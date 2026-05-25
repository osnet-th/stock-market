package com.thlee.stock.market.stockmarket.economics.derivedindicator.application.exception;

/**
 * 알 수 없는 프리셋 키 → 404.
 */
public class UnknownPresetException extends RuntimeException {
    public UnknownPresetException(String key) {
        super("알 수 없는 프리셋입니다: " + key);
    }
}
