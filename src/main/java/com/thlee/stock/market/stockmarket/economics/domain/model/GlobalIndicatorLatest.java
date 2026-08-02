package com.thlee.stock.market.stockmarket.economics.domain.model;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 글로벌 경제지표 최신값 도메인 모델 (변경 감지 + 이전값 보존)
 */
@Getter
public class GlobalIndicatorLatest {

    private final String countryName;
    private final GlobalEconomicIndicatorType indicatorType;
    private final String dataValue;
    private final String previousDataValue;
    private final String cycle;
    private final String unit;
    private final LocalDateTime updatedAt;
    // 마지막 수집 시각 — cycle 변경 여부와 무관하게 수집 성공마다 갱신 (#51 catch-up 판단 기준)
    private final LocalDateTime lastCollectedAt;

    public GlobalIndicatorLatest(String countryName,
                                  GlobalEconomicIndicatorType indicatorType,
                                  String dataValue,
                                  String previousDataValue,
                                  String cycle,
                                  String unit,
                                  LocalDateTime updatedAt,
                                  LocalDateTime lastCollectedAt) {
        this.countryName = countryName;
        this.indicatorType = indicatorType;
        this.dataValue = dataValue;
        this.previousDataValue = previousDataValue;
        this.cycle = cycle;
        this.unit = unit;
        this.updatedAt = updatedAt;
        this.lastCollectedAt = lastCollectedAt;
    }

    /**
     * CountryIndicatorSnapshot으로부터 최신값 생성 (초기 시딩용: previousDataValue = null)
     */
    public static GlobalIndicatorLatest fromSnapshot(CountryIndicatorSnapshot snapshot) {
        IndicatorValue lastValue = snapshot.getLastValue();
        LocalDateTime now = LocalDateTime.now();
        return new GlobalIndicatorLatest(
            snapshot.getCountryName(),
            snapshot.getIndicatorType(),
            lastValue != null ? lastValue.getRawText() : null,
            null,
            snapshot.getReferenceText(),
            lastValue != null ? lastValue.getUnit() : null,
            now,
            now
        );
    }

    /**
     * 비교 키 생성 (countryName + indicatorType)
     */
    public String toCompareKey() {
        return countryName + "::" + indicatorType.name();
    }
}