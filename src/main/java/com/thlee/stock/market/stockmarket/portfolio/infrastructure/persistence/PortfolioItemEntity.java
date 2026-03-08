package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "portfolio_item",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_portfolio_item",
                        columnNames = {"user_id", "item_name", "asset_type"}
                )
        },
        indexes = {
                @Index(name = "idx_portfolio_item_user_id", columnList = "user_id")
        }
)
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "asset_type", discriminatorType = DiscriminatorType.STRING)
@Getter
public abstract class PortfolioItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(name = "asset_type", insertable = false, updatable = false, length = 20)
    private String assetType;

    @Column(name = "invested_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal investedAmount;

    @Column(name = "news_enabled", nullable = false)
    private boolean newsEnabled;

    @Column(name = "region", nullable = false, length = 20)
    private String region;

    @Column(name = "memo", length = 500)
    private String memo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected PortfolioItemEntity() {
    }

    protected PortfolioItemEntity(Long id,
                                  Long userId,
                                  String itemName,
                                  BigDecimal investedAmount,
                                  boolean newsEnabled,
                                  String region,
                                  String memo,
                                  LocalDateTime createdAt,
                                  LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.itemName = itemName;
        this.investedAmount = investedAmount;
        this.newsEnabled = newsEnabled;
        this.region = region;
        this.memo = memo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}