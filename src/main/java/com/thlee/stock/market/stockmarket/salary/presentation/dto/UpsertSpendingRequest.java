package com.thlee.stock.market.stockmarket.salary.presentation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 카테고리별 지출 upsert 요청.
 */
@Getter
public class UpsertSpendingRequest {

    @NotNull(message = "금액은 필수입니다.")
    @DecimalMin(value = "0", inclusive = true, message = "금액은 0 이상이어야 합니다.")
    private BigDecimal amount;

    @Size(max = 200, message = "메모는 200자 이내로 입력해주세요.")
    private String memo;
}