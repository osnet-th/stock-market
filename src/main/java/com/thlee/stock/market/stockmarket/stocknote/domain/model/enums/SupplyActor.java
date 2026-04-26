package com.thlee.stock.market.stockmarket.stocknote.domain.model.enums;

/**
 * 수급 주체.
 *
 * <p><b>중요: Enum 상수 이름은 재명명 금지.</b>
 */
public enum SupplyActor {
    FOREIGN,          // 외국인 순매수
    INSTITUTION,      // 기관 순매수
    RETAIL,           // 개인 순매수
    SHORT_COVERING,   // 숏커버링
    ETF_FLOW          // ETF 편입 수급
}