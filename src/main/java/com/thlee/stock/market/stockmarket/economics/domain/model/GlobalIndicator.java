package com.thlee.stock.market.stockmarket.economics.domain.model;

import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 글로벌 경제지표 히스토리 도메인 모델
 */
@Getter
public class GlobalIndicator {

    private final Long id;
    private final String countryName;
    private final GlobalEconomicIndicatorType indicatorType;
    private final String dataValue;
    private final String cycle;
    private final String unit;
    private final LocalDate snapshotDate;
    private final LocalDateTime createdAt;

    public GlobalIndicator(Long id,
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

    /**
     * CountryIndicatorSnapshot으로부터 변환
     */
    public static GlobalIndicator fromSnapshot(CountryIndicatorSnapshot snapshot,
                                                LocalDate snapshotDate) {
        IndicatorValue lastValue = snapshot.getLastValue();
        return new GlobalIndicator(
            null,
            snapshot.getCountryName(),
            snapshot.getIndicatorType(),
            lastValue != null ? lastValue.getRawText() : null,
            snapshot.getReferenceText(),
            lastValue != null ? lastValue.getUnit() : null,
            snapshotDate,
            LocalDateTime.now()
        );
    }
}