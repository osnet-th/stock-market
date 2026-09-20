package com.thlee.stock.market.stockmarket.companyreport.domain.model;

import java.math.BigDecimal;
import java.util.List;

/** 저장 당시 계산 결과. 재무 스냅샷 갱신 및 읽기로 재계산하지 않는다. */
public record SrimValuation(int schemaVersion, int calculationVersion, SrimInput input,
                            List<YearResult> results, String calculatedAt) {
    public SrimValuation {
        results = List.copyOf(results);
    }

    public record YearResult(Integer year, BigDecimal roe, BigDecimal averageEquity,
                             Scenario perpetual, Scenario decline10, Scenario decline20) {}
    public record Scenario(BigDecimal price, BigDecimal differencePercent) {}
}
