package com.thlee.stock.market.stockmarket.economics.domain.model;

/**
 * ECOS 지표 dataValue(String, 천단위 쉼표 포함 가능) → Double 파싱 공유 헬퍼.
 * 순수 도메인 유틸 — Spring 의존 없음. 파싱 실패 시 null(null-safe).
 * EcosDerivedIndicatorService와 파생지표 평가 경로가 동일 규칙을 공유한다.
 */
public final class IndicatorValueParser {

    private IndicatorValueParser() {
    }

    public static Double parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
