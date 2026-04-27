package com.thlee.stock.market.stockmarket.portfolio.application.dto;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.StockSaleHistory;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.SaleReason;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
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

    private StockSaleHistoryResponse(Long id, Long portfolioItemId, int quantity,
                                     BigDecimal avgBuyPrice, BigDecimal salePrice,
                                     BigDecimal profit, BigDecimal profitRate, BigDecimal contributionRate,
                                     BigDecimal totalAssetAtSale, String currency, BigDecimal fxRate,
                                     BigDecimal salePriceKrw, BigDecimal profitKrw,
                                     SaleReason reason, String memo,
                                     String stockCode, String stockName, boolean unrecordedDeposit,
                                     LocalDate soldAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.portfolioItemId = portfolioItemId;
        this.quantity = quantity;
        this.avgBuyPrice = avgBuyPrice;
        this.salePrice = salePrice;
        this.profit = profit;
        this.profitRate = profitRate;
        this.contributionRate = contributionRate;
        this.totalAssetAtSale = totalAssetAtSale;
        this.currency = currency;
        this.fxRate = fxRate;
        this.salePriceKrw = salePriceKrw;
        this.profitKrw = profitKrw;
        this.reason = reason;
        this.memo = memo;
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.unrecordedDeposit = unrecordedDeposit;
        this.soldAt = soldAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

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