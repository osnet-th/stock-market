package com.thlee.stock.market.stockmarket.economics.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ECOS 경제지표 히스토리 JPA Entity
 */
@Entity
@Table(
    name = "ecos_indicator",
    indexes = {
        @Index(name = "idx_ecos_classname_keystat", columnList = "class_name, keystat_name"),
        @Index(name = "idx_ecos_snapshot_date", columnList = "snapshot_date")
    }
)
@Getter
public class EcosIndicatorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_name", nullable = false, length = 100)
    private String className;

    @Column(name = "keystat_name", nullable = false, length = 200)
    private String keystatName;

    @Column(name = "data_value", length = 50)
    private String dataValue;

    @Column(name = "cycle", length = 20)
    private String cycle;

    @Column(name = "unit_name", length = 50)
    private String unitName;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected EcosIndicatorEntity() {
    }

    public EcosIndicatorEntity(Long id,
                               String className,
                               String keystatName,
                               String dataValue,
                               String cycle,
                               String unitName,
                               LocalDate snapshotDate,
                               LocalDateTime createdAt) {
        this.id = id;
        this.className = className;
        this.keystatName = keystatName;
        this.dataValue = dataValue;
        this.cycle = cycle;
        this.unitName = unitName;
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
