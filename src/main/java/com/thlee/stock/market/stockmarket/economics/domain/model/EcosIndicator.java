package com.thlee.stock.market.stockmarket.economics.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ECOS 경제지표 히스토리 도메인 모델
 */
public class EcosIndicator {

    private final Long id;
    private final String className;
    private final String keystatName;
    private final String dataValue;
    private final String cycle;
    private final String unitName;
    private final LocalDate snapshotDate;
    private final LocalDateTime createdAt;

    public EcosIndicator(Long id,
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

    /**
     * KeyStatIndicator로부터 변환
     */
    public static EcosIndicator fromKeyStatIndicator(KeyStatIndicator indicator,
                                                      LocalDate snapshotDate) {
        return new EcosIndicator(
            null,
            indicator.className(),
            indicator.keystatName(),
            indicator.dataValue(),
            indicator.cycle(),
            indicator.unitName(),
            snapshotDate,
            LocalDateTime.now()
        );
    }

    public Long getId() { return id; }
    public String getClassName() { return className; }
    public String getKeystatName() { return keystatName; }
    public String getDataValue() { return dataValue; }
    public String getCycle() { return cycle; }
    public String getUnitName() { return unitName; }
    public LocalDate getSnapshotDate() { return snapshotDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
