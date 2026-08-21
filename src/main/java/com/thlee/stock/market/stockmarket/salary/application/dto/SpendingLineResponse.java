package com.thlee.stock.market.stockmarket.salary.application.dto;

import com.thlee.stock.market.stockmarket.salary.domain.model.SpendingConfig;
import com.thlee.stock.market.stockmarket.salary.domain.model.SpendingItem;
import com.thlee.stock.market.stockmarket.salary.domain.model.enums.SpendingCategory;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 특정 월의 카테고리별 지출 라인.
 *
 * <p>하위 항목이 있는 카테고리는 {@code amount}가 항목 합계의 파생값으로 저장돼 있다
 * (일괄 저장 경로가 유지하는 불변식).
 */
@Getter
public class SpendingLineResponse {

    private final SpendingCategory category;
    private final String categoryLabel;
    private final BigDecimal amount;
    private final String memo;

    /** 카테고리 월 예산 (미설정이면 null) */
    private final BigDecimal budget;

    /** 하위 항목 (세트 내 순서 유지, 없으면 빈 리스트) */
    private final List<SpendingItemResponse> items;

    /** null이면 해당 월에 직접 입력된 값, non-null이면 상속 출처 월 */
    private final YearMonth inheritedFromMonth;

    private SpendingLineResponse(SpendingCategory category, BigDecimal amount, String memo,
                                 BigDecimal budget, List<SpendingItemResponse> items,
                                 YearMonth inheritedFromMonth) {
        this.category = category;
        this.categoryLabel = category.getDisplayName();
        this.amount = amount;
        this.memo = memo;
        this.budget = budget;
        this.items = items;
        this.inheritedFromMonth = inheritedFromMonth;
    }

    public static SpendingLineResponse from(SpendingCategory category,
                                            SpendingConfig config,
                                            List<SpendingItem> items,
                                            YearMonth targetMonth) {
        List<SpendingItemResponse> itemResponses = toItemResponses(items);
        if (config == null) {
            return new SpendingLineResponse(category, BigDecimal.ZERO, null, null, itemResponses, null);
        }
        return new SpendingLineResponse(category, config.getAmount(), config.getMemo(),
                config.getBudget(), itemResponses, inheritedFrom(config, targetMonth));
    }

    private static List<SpendingItemResponse> toItemResponses(List<SpendingItem> items) {
        return items.stream()
                .map(SpendingItemResponse::from)
                .collect(Collectors.toList());
    }

    private static YearMonth inheritedFrom(SpendingConfig config, YearMonth targetMonth) {
        return config.getEffectiveFromMonth().equals(targetMonth) ? null : config.getEffectiveFromMonth();
    }
}
