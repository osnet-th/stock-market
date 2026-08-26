package com.thlee.stock.market.stockmarket.economics.derivedindicator.application.exception;

/**
 * 파생지표 미존재 또는 소유권 불일치 → 404. (존재 여부를 노출하지 않기 위해 소유권 위반도 404)
 */
public class DerivedIndicatorNotFoundException extends RuntimeException {
    public DerivedIndicatorNotFoundException(Long id) {
        super("파생지표를 찾을 수 없습니다: id=" + id);
    }
}
