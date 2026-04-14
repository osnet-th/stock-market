package com.thlee.stock.market.stockmarket.portfolio.presentation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class DepositRequest {
    private LocalDate depositDate;

    @NotNull(message = "납입 금액은 필수입니다.")
    @DecimalMin(value = "0.01", message = "납입 금액은 0보다 커야 합니다.")
    private BigDecimal amount;

    private BigDecimal units;

    @Size(max = 200, message = "메모는 200자 이내로 입력해주세요.")
    private String memo;
}