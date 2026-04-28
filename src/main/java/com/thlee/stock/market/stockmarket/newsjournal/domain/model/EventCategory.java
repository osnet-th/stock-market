package com.thlee.stock.market.stockmarket.newsjournal.domain.model;

/**
 * 사건이 시장/투자에 미치는 영향 분류.
 *
 * <p><b>중요: Enum 상수 이름은 재명명 금지.</b> 이 값은
 * {@code @Enumerated(EnumType.STRING)}으로 DB에 저장되므로, 이름을 바꾸면
 * 기존 행이 orphan이 된다. 추가는 허용하며, 제거 시 데이터 이관 스크립트 필수.
 */
public enum EventCategory {
    GOOD,    // 호재
    BAD,     // 악재
    NEUTRAL  // 중립
}