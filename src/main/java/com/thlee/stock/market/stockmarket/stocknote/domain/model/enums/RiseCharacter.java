package com.thlee.stock.market.stockmarket.stocknote.domain.model.enums;

/**
 * 등락의 성격 분류.
 *
 * <p><b>중요: Enum 상수 이름은 재명명 금지.</b> 이 값은
 * {@code @Enumerated(EnumType.STRING)}으로 DB에 저장되므로, 이름을 바꾸면
 * 기존 행이 orphan이 된다. 추가는 허용하며, 제거 시 데이터 이관 스크립트 필수.
 */
public enum RiseCharacter {
    FUNDAMENTAL,    // 실적형
    EXPECTATION,    // 기대형
    SUPPLY_DEMAND,  // 수급형
    THEME,          // 테마형
    REVALUATION     // 밸류에이션 리레이팅형
}