package com.thlee.stock.market.stockmarket.salary.presentation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 월급 upsert 요청.
 */
@Getter
public class UpsertIncomeRequest {

    @NotNull(message = "금액은 필수입니다.")
    @DecimalMin(value = "0", inclusive = true, message = "금액은 0 이상이어야 합니다.")
    private BigDecimal amount;
}