package com.thlee.stock.market.stockmarket.realestate.presentation.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 카드 단위 표시. 판단/등급 라벨 절대 금지 — 수치/메타만.
 */
public record IndicatorCard(String indicatorCode,
                            String displayName,
                            String description,
                            String unit,
                            String source,
                            String sourceUrl,
                            BigDecimal value,
                            BigDecimal previousValue,
                            BigDecimal changeRate,
                            String referenceText,
                            String previousReferenceText,
                            String snapshotDate,
                            boolean estimated,
                            List<HistoryPoint> history) {
}