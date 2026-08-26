package com.thlee.stock.market.stockmarket.stock.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * SEC companyfacts 기반 연간 재무 팩트 (미국 리포트 조달용).
 *
 * @param years  오름차순 정렬된 사업연도 목록 (최근 N년, 예: ["2016", ..., "2025"])
 * @param annual 개념별 연도→값 시리즈. 금액은 달러, EPS/DPS는 주당 달러, 주식수는 주. 미검출 개념/연도는 부재 (0 대체 금지)
 */
public record UsCompanyFacts(
        List<String> years,
        Map<UsFinancialConcept, Map<String, BigDecimal>> annual
) {

    public Map<String, BigDecimal> series(UsFinancialConcept concept) {
        return annual.getOrDefault(concept, Map.of());
    }

    public BigDecimal valueAt(UsFinancialConcept concept, String year) {
        return series(concept).get(year);
    }
}
