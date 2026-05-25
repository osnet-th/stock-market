package com.thlee.stock.market.stockmarket.realestate.presentation.dto;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RegionLevel;

import java.math.BigDecimal;
import java.util.List;

/**
 * 카드 단위 표시. 판단/등급 라벨 절대 금지 — 수치/메타만.
 * <p>
 * {@code regionLevel}은 카드가 광역(SIDO)/시군구(SIGUNGU) 중 어느 단위인지 derive되는 메타.
 * FE는 이 값으로 카드 그룹(섹션) 분기에 사용.
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
                            RegionLevel regionLevel,
                            List<HistoryPoint> history) {
}
