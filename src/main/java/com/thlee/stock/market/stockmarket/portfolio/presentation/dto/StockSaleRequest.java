package com.thlee.stock.market.stockmarket.portfolio.presentation.dto;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.SaleReason;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class StockSaleRequest {

    @NotNull
    @Min(1)
    private Integer quantity;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal salePrice;

    @NotNull
    @PastOrPresent
    private LocalDate soldAt;

    @NotNull
    private SaleReason reason;

    @Size(max = 200)
    private String memo;

    /** 외화 종목용. null이면 서버가 자동 조회 */
    private BigDecimal fxRate;

    /** CashStockLink 미연결 시 사용자가 선택한 입금 대상. null이면 자동 결정/unrecorded */
    private Long depositCashItemId;
}