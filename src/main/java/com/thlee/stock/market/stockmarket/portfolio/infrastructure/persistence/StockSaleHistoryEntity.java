package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.SaleReason;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "stock_sale_history",
        indexes = {
                @Index(name = "idx_stock_sale_history_item_solddate", columnList = "portfolio_item_id, sold_at"),
                @Index(name = "idx_stock_sale_history_sold_at", columnList = "sold_at")
        }
)
@Getter
public class StockSaleHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "portfolio_item_id", nullable = false)
    private Long portfolioItemId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "avg_buy_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal avgBuyPrice;

    @Column(name = "sale_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "profit", nullable = false, precision = 18, scale = 2)
    private BigDecimal profit;

    @Column(name = "profit_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal profitRate;

    @Column(name = "contribution_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal contributionRate;

    @Column(name = "total_asset_at_sale", precision = 18, scale = 2)
    private BigDecimal totalAssetAtSale;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "fx_rate", precision = 18, scale = 6)
    private BigDecimal fxRate;

    @Column(name = "sale_price_krw", precision = 18, scale = 2)
    private BigDecimal salePriceKrw;

    @Column(name = "profit_krw", precision = 18, scale = 2)
    private BigDecimal profitKrw;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 40)
    private SaleReason reason;

    @Column(name = "memo", length = 200)
    private String memo;

    @Column(name = "stock_code", length = 20)
    private String stockCode;

    @Column(name = "stock_name", length = 100)
    private String stockName;

    @Column(name = "unrecorded_deposit", nullable = false)
    private boolean unrecordedDeposit;

    @Column(name = "sold_at", nullable = false)
    private LocalDate soldAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected StockSaleHistoryEntity() {
    }

    public StockSaleHistoryEntity(Long id,
                                  Long portfolioItemId,
                                  int quantity,
                                  BigDecimal avgBuyPrice,
                                  BigDecimal salePrice,
                                  BigDecimal profit,
                                  BigDecimal profitRate,
                                  BigDecimal contributionRate,
                                  BigDecimal totalAssetAtSale,
                                  String currency,
                                  BigDecimal fxRate,
                                  BigDecimal salePriceKrw,
                                  BigDecimal profitKrw,
                                  SaleReason reason,
                                  String memo,
                                  String stockCode,
                                  String stockName,
                                  boolean unrecordedDeposit,
                                  LocalDate soldAt,
                                  LocalDateTime createdAt,
                                  LocalDateTime updatedAt) {
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
}