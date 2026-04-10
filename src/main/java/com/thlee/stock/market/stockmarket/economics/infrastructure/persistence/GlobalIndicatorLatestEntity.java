package com.thlee.stock.market.stockmarket.economics.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalEconomicIndicatorType;
import jakarta.persistence.*;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 글로벌 경제지표 최신값 JPA Entity (변경 감지 비교용)
 */
@Entity
@Table(name = "global_indicator_latest")
@IdClass(GlobalIndicatorLatestEntity.LatestId.class)
@Getter
public class GlobalIndicatorLatestEntity {

    @Id
    @Column(name = "country_name", nullable = false, length = 100)
    private String countryName;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "indicator_type", nullable = false, length = 100)
    private GlobalEconomicIndicatorType indicatorType;

    @Column(name = "data_value", length = 50)
    private String dataValue;

    @Column(name = "previous_data_value", length = 50)
    private String previousDataValue;

    @Column(name = "cycle", length = 50)
    private String cycle;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected GlobalIndicatorLatestEntity() {
    }

    public GlobalIndicatorLatestEntity(String countryName,
                                        GlobalEconomicIndicatorType indicatorType,
                                        String dataValue,
                                        String previousDataValue,
                                        String cycle,
                                        String unit,
                                        LocalDateTime updatedAt) {
        this.countryName = countryName;
        this.indicatorType = indicatorType;
        this.dataValue = dataValue;
        this.previousDataValue = previousDataValue;
        this.cycle = cycle;
        this.unit = unit;
        this.updatedAt = updatedAt;
    }

    /**
     * 값 갱신 — cycle 변경 시 이전값 보존
     */
    public void update(String newDataValue,
                       String newCycle,
                       String newUnit,
                       boolean cycleChanged,
                       LocalDateTime updatedAt) {
        if (cycleChanged) {
            this.previousDataValue = this.dataValue;
        }
        this.dataValue = newDataValue;
        this.cycle = newCycle;
        this.unit = newUnit;
        this.updatedAt = updatedAt;
    }

    /**
     * 복합 PK 클래스
     */
    public static class LatestId implements Serializable {
        private String countryName;
        private GlobalEconomicIndicatorType indicatorType;

        public LatestId() {
        }

        public LatestId(String countryName, GlobalEconomicIndicatorType indicatorType) {
            this.countryName = countryName;
            this.indicatorType = indicatorType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LatestId latestId = (LatestId) o;
            return Objects.equals(countryName, latestId.countryName)
                && indicatorType == latestId.indicatorType;
        }

        @Override
        public int hashCode() {
            return Objects.hash(countryName, indicatorType);
        }
    }
}