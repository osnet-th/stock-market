package com.thlee.stock.market.stockmarket.economics.domain.model;

/**
 * 경제지표 단건 도메인 모델
 */
public record KeyStatIndicator(
    String className,
    String keystatName,
    String dataValue,
    String cycle,
    String unitName
) {

    /**
     * 비교 키 생성 (className + keystatName)
     */
    public String toCompareKey() {
        return className + "::" + keystatName;
    }
}