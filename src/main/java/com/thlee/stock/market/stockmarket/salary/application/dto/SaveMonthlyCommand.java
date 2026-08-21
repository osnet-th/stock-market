package com.thlee.stock.market.stockmarket.salary.application.dto;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 해당 월 일괄 저장 커맨드 — 월급 + 카테고리 구조/금액·예산 + 하위 항목 세트 + 저축률 목표.
 *
 * <p>전체 폼 스냅샷 계약: payload의 카테고리 목록이 그 달의 활성 카테고리 전체를 나타낸다.
 * code 없는 항목은 신규 생성, 목록에서 빠진 활성 카테고리는 비활성 처리된다.
 */
@Getter
public class SaveMonthlyCommand {

    /** 월 실수령액. null이면 월급은 변경하지 않는다. */
    private final BigDecimal income;

    /** 저축률 목표(%). null이면 변경하지 않는다. */
    private final Integer savingTarget;

    private final List<CategoryCommand> categories;

    public SaveMonthlyCommand(BigDecimal income, Integer savingTarget, List<CategoryCommand> categories) {
        this.income = income;
        this.savingTarget = savingTarget;
        this.categories = List.copyOf(categories);
    }

    @Getter
    public static class CategoryCommand {

        /** 기존 카테고리 code. null이면 신규 생성(동명 inactive는 재활성). */
        private final String category;

        /** 카테고리 이름 — 신규 생성/커스텀 이름 변경용. system 카테고리는 무시. */
        private final String name;

        /** 항목이 없을 때의 직접 입력 금액. 항목이 있으면 무시된다. */
        private final BigDecimal amount;

        private final BigDecimal budget;

        /** 저축률 산입 여부 — 커스텀 카테고리만 반영, null이면 미변경. */
        private final Boolean savings;

        private final List<ItemCommand> items;

        public CategoryCommand(String category, String name, BigDecimal amount,
                               BigDecimal budget, Boolean savings, List<ItemCommand> items) {
            this.category = category;
            this.name = name;
            this.amount = amount;
            this.budget = budget;
            this.savings = savings;
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
