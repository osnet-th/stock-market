package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "crypto_detail")
@DiscriminatorValue("CRYPTO")
public class CryptoItemEntity extends PortfolioItemEntity {

    protected CryptoItemEntity() {
    }

    public CryptoItemEntity(Long id,
                            Long userId,
                            String itemName,
                            BigDecimal investedAmount,
                            boolean newsEnabled,
                            String region,
                            String memo,
                            LocalDateTime createdAt,
                            LocalDateTime updatedAt) {
        super(id, userId, itemName, investedAmount, newsEnabled, region, memo, createdAt, updatedAt);
    }
}