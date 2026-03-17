package com.thlee.stock.market.stockmarket.economics.domain.model;

/**
 * 경제지표 단건 도메인 모델
 */
public record KeyStatIndicator(
    String className,
    String keystatName,
    String dataValue,
    String previousDataValue,
    String cycle,
    String unitName
) {

    /**
     * 비교 키 생성 (className + keystatName)
     */
    public String toCompareKey() {
        return className + "::" + keystatName;
    }

    /**
     * previousDataValue를 병합한 새 인스턴스 생성
     */
    public KeyStatIndicator withPreviousDataValue(String previousDataValue) {
        return new KeyStatIndicator(
            className, keystatName, dataValue, previousDataValue, cycle, unitName
        );
    }
}