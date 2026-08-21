package com.thlee.stock.market.stockmarket.salary.application.dto;

import com.thlee.stock.market.stockmarket.salary.domain.model.SpendingConfig;
import com.thlee.stock.market.stockmarket.salary.domain.model.SpendingItem;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 특정 월의 카테고리별 지출 라인.
 *
 * <p>하위 항목이 있는 카테고리는 {@code amount}가 항목 합계의 파생값으로 저장돼 있다
 * (일괄 저장 경로가 유지하는 불변식). 카테고리 메타(이름·색·savings·system)를 함께 실어
 * 화면이 사용자 정의 카테고리를 동적으로 렌더한다.
 */
@Getter
public class SpendingLineResponse {

    /** 사용자 카테고리 code (기본 8종은 SpendingCategory enum 이름과 동일) */
    private final String category;
    private final String categoryLabel;
    private final String color;
    private final boolean savings;
    private final boolean system;

    /** soft delete 된 카테고리가 과거 월 이력으로 표시되는 경우 false */
    private final boolean active;

    private final BigDecimal amount;
    private final String memo;

    /** 카테고리 월 예산 (미설정이면 null) */
    private final BigDecimal budget;

    /** 하위 항목 (세트 내 순서 유지, 없으면 빈 리스트) */
    private final List<SpendingItemResponse> items;

    /** null이면 해당 월에 직접 입력된 값, non-null이면 상속 출처 월 */
    private final YearMonth inheritedFromMonth;

    private SpendingLineResponse(CategoryMetaResponse meta, BigDecimal amount, String memo,
                                 BigDecimal budget, List<SpendingItemResponse> items,
                                 YearMonth inheritedFromMonth) {
        this.category = meta.getCode();
        this.categoryLabel = meta.getName();
        this.color = meta.getColor();
        this.savings = meta.isSavings();
        this.system = meta.isSystem();
        this.active = meta.isActive();
        this.amount = amount;
        this.memo = memo;
        this.budget = budget;
        this.items = items;
        this.inheritedFromMonth = inheritedFromMonth;
    }

    public static SpendingLineResponse from(CategoryMetaResponse meta,
                                            SpendingConfig config,
                                            List<SpendingItem> items,
                                            YearMonth targetMonth) {
        List<SpendingItemResponse> itemResponses = toItemResponses(items);
        if (config == null) {
            return new SpendingLineResponse(meta, BigDecimal.ZERO, null, null, itemResponses, null);
        }
        return new SpendingLineResponse(meta, config.getAmount(), config.getMemo(),
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
