package com.thlee.stock.market.stockmarket.salary.application.dto;

import com.thlee.stock.market.stockmarket.salary.domain.model.UserSpendingCategory;
import lombok.Getter;

/**
 * 카테고리 메타 — 코드·이름·색·저축 여부. 추이 `구성` 모드와 화면 렌더용.
 */
@Getter
public class CategoryMetaResponse {

    private final String code;
    private final String name;
    private final String color;
    private final boolean savings;
    private final boolean system;
    private final boolean active;

    public CategoryMetaResponse(String code, String name, String color,
                                boolean savings, boolean system, boolean active) {
        this.code = code;
        this.name = name;
        this.color = color;
        this.savings = savings;
        this.system = system;
        this.active = active;
    }

    public static CategoryMetaResponse from(UserSpendingCategory category) {
        return new CategoryMetaResponse(category.getCode(), category.getName(), category.getColor(),
                category.isSavings(), category.isSystem(), category.isActive());
    }

    /** 메타가 유실된 code 방어용 (표시는 code 그대로, 회색) */
    public static CategoryMetaResponse unknown(String code) {
        return new CategoryMetaResponse(code, code, "#7a828a", false, false, false);
    }
}
