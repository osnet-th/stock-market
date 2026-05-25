package com.thlee.stock.market.stockmarket.economics.derivedindicator.application.exception;

/**
 * 동일 사용자 내 파생지표 이름 중복 → 409.
 */
public class DuplicateDerivedIndicatorNameException extends RuntimeException {
    public DuplicateDerivedIndicatorNameException(String name) {
        super("이미 같은 이름의 파생지표가 있습니다: " + name);
    }
}
