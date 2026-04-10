package com.thlee.stock.market.stockmarket.economics.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalEconomicIndicatorType;
import jakarta.persistence.*;
import lombok.Getter;

/**
 * 글로벌 경제지표 메타데이터 JPA Entity
 */
@Entity
@Table(name = "global_indicator_metadata")
@Getter
public class GlobalIndicatorMetadataEntity {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "indicator_type", nullable = false, length = 100)
    private GlobalEconomicIndicatorType indicatorType;

    @Column(name = "description", length = 500)
    private String description;

    protected GlobalIndicatorMetadataEntity() {
    }

    public GlobalIndicatorMetadataEntity(GlobalEconomicIndicatorType indicatorType,
                                          String description) {
        this.indicatorType = indicatorType;
        this.description = description;
    }
}