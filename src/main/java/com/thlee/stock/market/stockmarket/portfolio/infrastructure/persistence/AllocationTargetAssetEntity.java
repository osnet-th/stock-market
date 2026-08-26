package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;

@Entity
@Table(
        name = "allocation_target_asset",
        indexes = {
                @Index(name = "idx_allocation_target_asset_target_id", columnList = "allocation_target_id")
        }
)
@Getter
public class AllocationTargetAssetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "allocation_target_id", nullable = false)
    private Long allocationTargetId;

    @Column(name = "asset_type", nullable = false, length = 20)
    private String assetType;

    @Column(name = "target_ratio", nullable = false, precision = 5, scale = 2)
    private BigDecimal targetRatio;

    protected AllocationTargetAssetEntity() {
    }

    public AllocationTargetAssetEntity(Long id,
                                       Long allocationTargetId,
                                       String assetType,
                                       BigDecimal targetRatio) {
        this.id = id;
        this.allocationTargetId = allocationTargetId;
        this.assetType = assetType;
        this.targetRatio = targetRatio;
    }
}
