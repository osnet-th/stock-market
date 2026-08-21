package com.thlee.stock.market.stockmarket.salary.presentation.dto;

import com.thlee.stock.market.stockmarket.salary.application.dto.SaveMonthlyCommand;
import com.thlee.stock.market.stockmarket.salary.domain.model.enums.SpendingCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 해당 월 일괄 저장 요청 — 월급 + 카테고리(금액·예산) + 하위 항목 세트.
 * 검증 후 {@link SaveMonthlyCommand}로 변환해 application 계층에 전달한다.
 */
@Getter
public class SaveMonthlyRequest {

    /** 월 실수령액. null이면 월급은 변경하지 않는다. */
    @DecimalMin(value = "0", inclusive = true, message = "월급은 0 이상이어야 합니다.")
    private BigDecimal income;

    @Valid
    @Size(max = 20, message = "카테고리는 20개 이하여야 합니다.")
    private List<CategoryPayload> categories = new ArrayList<>();

    public SaveMonthlyCommand toCommand() {
        return new SaveMonthlyCommand(income,
                categories.stream().map(CategoryPayload::toCommand).collect(Collectors.toList()));
    }

    @Getter
    public static class CategoryPayload {

        @NotNull(message = "카테고리는 필수입니다.")
        private SpendingCategory category;

        /** 항목이 없을 때의 직접 입력 금액. 항목이 있으면 무시된다. */
        @DecimalMin(value = "0", inclusive = true, message = "금액은 0 이상이어야 합니다.")
        private BigDecimal amount;

        @DecimalMin(value = "0", inclusive = true, message = "예산은 0 이상이어야 합니다.")
        private BigDecimal budget;

        @Valid
        @Size(max = 100, message = "항목은 카테고리당 100개 이하여야 합니다.")
        private List<ItemPayload> items = new ArrayList<>();

        private SaveMonthlyCommand.CategoryCommand toCommand() {
            return new SaveMonthlyCommand.CategoryCommand(category, amount, budget,
                    items.stream().map(ItemPayload::toCommand).collect(Collectors.toList()));
        }
    }

    @Getter
    public static class ItemPayload {

        @Size(max = 100, message = "항목 이름은 100자 이내로 입력해주세요.")
        private String name;

        @NotNull(message = "항목 금액은 필수입니다.")
        @DecimalMin(value = "0", inclusive = true, message = "항목 금액은 0 이상이어야 합니다.")
        private BigDecimal amount;

        private boolean fixed;

        private SaveMonthlyCommand.ItemCommand toCommand() {
            return new SaveMonthlyCommand.ItemCommand(name, amount, fixed);
        }
    }
}
