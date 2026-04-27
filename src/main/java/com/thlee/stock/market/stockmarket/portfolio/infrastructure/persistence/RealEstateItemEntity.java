package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.PortfolioItemStatus;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "real_estate_detail")
@DiscriminatorValue("REAL_ESTATE")
@Getter
public class RealEstateItemEntity extends PortfolioItemEntity {

    @Column(name = "sub_type", length = 20)
    private String subType;

    @Column(name = "address", length = 200)
    private String address;

    @Column(name = "area", precision = 10, scale = 2)
    private BigDecimal area;

    protected RealEstateItemEntity() {
    }

    public RealEstateItemEntity(Long id,
                                Long userId,
                                String itemName,
                                BigDecimal investedAmount,
                                boolean newsEnabled,
                                String region,
                                String memo,
                                PortfolioItemStatus status,
                                Long version,
                                LocalDateTime createdAt,
                                LocalDateTime updatedAt,
                                String subType,
                                String address,
                                BigDecimal area) {
        super(id, userId, itemName, investedAmount, newsEnabled, region, memo, status, version, createdAt, updatedAt);
        this.subType = subType;
        this.address = address;
        this.area = area;
    }
}