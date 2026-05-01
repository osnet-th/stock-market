package com.thlee.stock.market.stockmarket.salary.domain.model.enums;

import lombok.Getter;

/**
 * 월별 지출 카테고리 (8개 고정).
 *
 * <p><b>중요: Enum 상수 이름은 재명명 금지.</b> 이 값은
 * {@code @Enumerated(EnumType.STRING)}으로 DB에 저장되므로, 이름을 바꾸면
 * 기존 행이 orphan이 된다. 추가는 허용하며, 제거 시 데이터 이관 스크립트 필수.
 */
@Getter
public enum SpendingCategory {
    FOOD("식비"),
    HOUSING("주거"),
    TRANSPORT("교통"),
    EVENTS("경조사"),
    COMMUNICATION("통신"),
    LEISURE("여가"),
    SAVINGS_INVESTMENT("저축·투자"),
    ETC("기타");

    private final String displayName;

    SpendingCategory(String displayName) {
        this.displayName = displayName;
    }
}
