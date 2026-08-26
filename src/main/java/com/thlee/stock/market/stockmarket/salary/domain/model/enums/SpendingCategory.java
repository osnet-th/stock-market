package com.thlee.stock.market.stockmarket.salary.domain.model.enums;

import lombok.Getter;

/**
 * 기본 지출 카테고리 시드 카탈로그 (8종).
 *
 * <p>카테고리는 사용자 정의 테이블({@code user_spending_category})로 전환되었고,
 * 본 enum은 사용자별 최초 시드의 원천으로만 쓰인다. 기존 행들이 enum 이름을
 * code('FOOD' 등)로 저장하고 있으므로 <b>상수 이름 재명명 금지</b>. 추가는 허용.
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
