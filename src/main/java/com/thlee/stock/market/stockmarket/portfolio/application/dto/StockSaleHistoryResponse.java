package com.thlee.stock.market.stockmarket.portfolio.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.StockSaleHistory;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.SaleReason;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class StockSaleHistoryResponse {

    private final Long id;
    private final Long portfolioItemId;
    private final int quantity;
    private final BigDecimal avgBuyPrice;
    private final BigDecimal salePrice;
    private final BigDecimal profit;
    private final BigDecimal profitRate;
    private final BigDecimal contributionRate;
    private final BigDecimal totalAssetAtSale;
    private final String currency;
    private final BigDecimal fxRate;
    private final BigDecimal salePriceKrw;
    private final BigDecimal profitKrw;
    private final SaleReason reason;
    private final String memo;
    private final String stockCode;
    private final String stockName;
    private final boolean unrecordedDeposit;
    private final LocalDate soldAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static StockSaleHistoryResponse from(StockSaleHistory history) {
        return new StockSaleHistoryResponse(
                history.getId(),
                history.getPortfolioItemId(),
                history.getQuantity(),
                history.getAvgBuyPrice(),
                history.getSalePrice(),
                history.getProfit(),
                history.getProfitRate(),
                history.getContributionRate(),
                history.getTotalAssetAtSale(),
                history.getCurrency(),
                history.getFxRate(),
                history.getSalePriceKrw(),
                history.getProfitKrw(),
                history.getReason(),
                history.getMemo(),
                history.getStockCode(),
                history.getStockName(),
                history.isUnrecordedDeposit(),
                history.getSoldAt(),
                history.getCreatedAt(),
                history.getUpdatedAt()
        );
    }
}