package com.thlee.stock.market.stockmarket.salary.presentation.dto;

import com.thlee.stock.market.stockmarket.salary.application.dto.SaveMonthlyCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 해당 월 일괄 저장 요청 — 월급 + 카테고리 구조/금액·예산 + 하위 항목 세트 + 저축률 목표.
 * 검증 후 {@link SaveMonthlyCommand}로 변환해 application 계층에 전달한다.
 *
 * <p>전체 폼 스냅샷 계약: categories가 그 달의 활성 카테고리 전체다. category(code)가 없으면
 * 신규 생성, 목록에서 빠진 활성 카테고리는 비활성 처리된다(저축 카테고리는 제외 불가).
 */
@Getter
public class SaveMonthlyRequest {

    /** 월 실수령액. null이면 월급은 변경하지 않는다. */
    @DecimalMin(value = "0", inclusive = true, message = "월급은 0 이상이어야 합니다.")
    private BigDecimal income;

    /** 저축률 목표(%). null이면 변경하지 않는다. */
    @Min(value = 0, message = "저축률 목표는 0 이상이어야 합니다.")
    @Max(value = 100, message = "저축률 목표는 100 이하여야 합니다.")
    private Integer savingTarget;

    @Valid
    @Size(max = 30, message = "카테고리는 30개 이하여야 합니다.")
    private List<CategoryPayload> categories = new ArrayList<>();

    public SaveMonthlyCommand toCommand() {
        return new SaveMonthlyCommand(income, savingTarget,
                categories.stream().map(CategoryPayload::toCommand).collect(Collectors.toList()));
    }

    @Getter
    public static class CategoryPayload {

        /** 기존 카테고리 code. null이면 신규 생성. */
        @Size(max = 20, message = "카테고리 코드가 올바르지 않습니다.")
        private String category;

        /** 카테고리 이름 — 신규 생성/커스텀 이름 변경용. */
        @Size(max = 40, message = "카테고리 이름은 40자 이내로 입력해주세요.")
        private String name;

        /** 항목이 없을 때의 직접 입력 금액. 항목이 있으면 무시된다. */
        @DecimalMin(value = "0", inclusive = true, message = "금액은 0 이상이어야 합니다.")
        private BigDecimal amount;

        @DecimalMin(value = "0", inclusive = true, message = "예산은 0 이상이어야 합니다.")
        private BigDecimal budget;

        /** 저축률 산입 여부 — 커스텀 카테고리만 반영, null이면 미변경. */
        private Boolean savings;

        @Valid
        @Size(max = 100, message = "항목은 카테고리당 100개 이하여야 합니다.")
        private List<ItemPayload> items = new ArrayList<>();

        private SaveMonthlyCommand.CategoryCommand toCommand() {
            return new SaveMonthlyCommand.CategoryCommand(category, name, amount, budget, savings,
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
