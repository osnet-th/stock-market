package com.thlee.stock.market.stockmarket.economics.domain.model;

import lombok.Getter;

/**
 * ECOS 경제지표 메타데이터 도메인 모델
 */
@Getter
public class EcosIndicatorMetadata {

    private final String className;
    private final String keystatName;
    private final String description;
    private final PositiveDirection positiveDirection;
    private final boolean keyIndicator;

    public EcosIndicatorMetadata(String className,
                                  String keystatName,
                                  String description,
                                  PositiveDirection positiveDirection,
                                  boolean keyIndicator) {
        this.className = className;
        this.keystatName = keystatName;
        this.description = description;
        this.positiveDirection = positiveDirection;
        this.keyIndicator = keyIndicator;
    }

    /**
     * compareKey 생성 — EcosIndicatorLatest, KeyStatIndicator와 동일 패턴
     */
    public String toCompareKey() {
        return className + "::" + keystatName;
    }

    public enum PositiveDirection {
        UP, DOWN, NEUTRAL
    }
}