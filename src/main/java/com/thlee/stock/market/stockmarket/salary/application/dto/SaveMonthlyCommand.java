package com.thlee.stock.market.stockmarket.salary.application.dto;

import com.thlee.stock.market.stockmarket.salary.domain.model.enums.SpendingCategory;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 해당 월 일괄 저장 커맨드 — 월급 + 카테고리(금액·예산) + 하위 항목 세트.
 */
@Getter
public class SaveMonthlyCommand {

    /** 월 실수령액. null이면 월급은 변경하지 않는다. */
    private final BigDecimal income;

    private final List<CategoryCommand> categories;

    public SaveMonthlyCommand(BigDecimal income, List<CategoryCommand> categories) {
        this.income = income;
        this.categories = List.copyOf(categories);
    }

    @Getter
    public static class CategoryCommand {

        private final SpendingCategory category;

        /** 항목이 없을 때의 직접 입력 금액. 항목이 있으면 무시된다. */
        private final BigDecimal amount;

        private final BigDecimal budget;

        private final List<ItemCommand> items;

        public CategoryCommand(SpendingCategory category, BigDecimal amount,
                               BigDecimal budget, List<ItemCommand> items) {
            this.category = category;
            this.amount = amount;
            this.budget = budget;
            this.items = List.copyOf(items);
        }

        /** 저장할 카테고리 금액 — 항목이 있으면 합계, 없으면 직접 입력값(미입력 시 0). */
        public BigDecimal resolvedAmount() {
            if (items.isEmpty()) {
                return amount != null ? amount : BigDecimal.ZERO;
            }
            return items.stream().map(ItemCommand::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    @Getter
    public static class ItemCommand {

        private final String name;
        private final BigDecimal amount;
        private final boolean fixed;

        public ItemCommand(String name, BigDecimal amount, boolean fixed) {
            this.name = name;
            this.amount = amount;
            this.fixed = fixed;
        }
    }
}
