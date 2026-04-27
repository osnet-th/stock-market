package com.thlee.stock.market.stockmarket.portfolio.presentation.dto;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.SaleReason;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class StockSaleHistoryUpdateRequest {

    @NotNull
    @Min(1)
    private Integer quantity;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal salePrice;

    @NotNull
    private SaleReason reason;

    @Size(max = 200)
    private String memo;
}