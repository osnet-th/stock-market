package com.thlee.stock.market.stockmarket.stocknote.domain.model.enums;

/**
 * 등락의 직접 트리거 유형.
 *
 * <p><b>중요: Enum 상수 이름은 재명명 금지.</b>
 */
public enum TriggerType {
    DISCLOSURE,   // 공시
    EARNINGS,     // 실적
    NEWS,         // 뉴스
    POLICY,       // 정책
    INDUSTRY,     // 업황
    SUPPLY,       // 수급
    THEME,        // 테마
    ETC           // 기타
}