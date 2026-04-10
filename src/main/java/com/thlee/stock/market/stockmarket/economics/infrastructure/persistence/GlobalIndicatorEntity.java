package com.thlee.stock.market.stockmarket.economics.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalEconomicIndicatorType;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 글로벌 경제지표 히스토리 JPA Entity
 */
@Entity
@Table(
    name = "global_indicator",
    indexes = {
        @Index(name = "idx_global_country_indicator", columnList = "country_name, indicator_type"),
        @Index(name = "idx_global_snapshot_date", columnList = "snapshot_date"),
        @Index(name = "idx_global_group_snapshot", columnList = "country_name, indicator_type, cycle, snapshot_date DESC")
    }
)
@Getter
public class GlobalIndicatorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "country_name", nullable = false, length = 100)
    private String countryName;

    @Enumerated(EnumType.STRING)
    @Column(name = "indicator_type", nullable = false, length = 100)
    private GlobalEconomicIndicatorType indicatorType;

    @Column(name = "data_value", length = 50)
    private String dataValue;

    @Column(name = "cycle", length = 50)
    private String cycle;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected GlobalIndicatorEntity() {
    }

    public GlobalIndicatorEntity(Long id,
                                  String countryName,
                                  GlobalEconomicIndicatorType indicatorType,
                                  String dataValue,
                                  String cycle,
                                  String unit,
                                  LocalDate snapshotDate,
                                  LocalDateTime createdAt) {
        this.id = id;
        this.countryName = countryName;
        this.indicatorType = indicatorType;
        this.dataValue = dataValue;
        this.cycle = cycle;
        this.unit = unit;
        this.snapshotDate = snapshotDate;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}