package com.thlee.stock.market.stockmarket.salary.application.dto;

import com.thlee.stock.market.stockmarket.salary.domain.model.SpendingConfig;
import com.thlee.stock.market.stockmarket.salary.domain.model.enums.SpendingCategory;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * 특정 월의 카테고리별 지출 라인.
 */
@Getter
public class SpendingLineResponse {

    private final SpendingCategory category;
    private final BigDecimal amount;
    private final String memo;

    /** null이면 해당 월에 직접 입력된 값, non-null이면 상속 출처 월 */
    private final YearMonth inheritedFromMonth;

    private SpendingLineResponse(SpendingCategory category, BigDecimal amount, String memo,
                                 YearMonth inheritedFromMonth) {
        this.category = category;
        this.amount = amount;
        this.memo = memo;
        this.inheritedFromMonth = inheritedFromMonth;
    }

    public static SpendingLineResponse from(SpendingCategory category,
                                            SpendingConfig config,
                                            YearMonth targetMonth) {
        if (config == null) {
            return new SpendingLineResponse(category, BigDecimal.ZERO, null, null);
        }
        YearMonth inherited = config.getEffectiveFromMonth().equals(targetMonth)
                ? null
                : config.getEffectiveFromMonth();
        return new SpendingLineResponse(category, config.getAmount(), config.getMemo(), inherited);
    }
}