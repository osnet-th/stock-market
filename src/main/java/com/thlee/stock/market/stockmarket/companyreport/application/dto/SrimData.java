package com.thlee.stock.market.stockmarket.companyreport.application.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.thlee.stock.market.stockmarket.companyreport.domain.model.SrimValuation;

import java.math.BigDecimal;
import java.util.List;

/** API/저장 전용 표현. ISO 날짜/시각 문자열을 사용한다. */
public record SrimData(int schemaVersion, int calculationVersion, SrimInputData input,
                       List<YearResult> results, String calculatedAt) {
    public static SrimData from(SrimValuation v) {
        return v == null ? null : new SrimData(v.schemaVersion(), v.calculationVersion(),
                SrimInputData.from(v.input()), v.results().stream().map(YearResult::from).toList(), v.calculatedAt());
    }
    public SrimValuation toDomain() {
        return new SrimValuation(schemaVersion, calculationVersion, input.toDomain(),
                results.stream().map(YearResult::toDomain).toList(), calculatedAt);
    }
    public record YearResult(
            Integer year,
            @JsonSerialize(using = SrimDecimalSerializer.class) BigDecimal roe,
            @JsonSerialize(using = SrimDecimalSerializer.class) BigDecimal averageEquity,
            Scenario perpetual, Scenario decline10, Scenario decline20
    ) {
        static YearResult from(SrimValuation.YearResult r) {
            return new YearResult(r.year(), r.roe(), r.averageEquity(), Scenario.from(r.perpetual()),
                    Scenario.from(r.decline10()), Scenario.from(r.decline20()));
        }
        SrimValuation.YearResult toDomain() {
            return new SrimValuation.YearResult(year, roe, averageEquity, Scenario.domain(perpetual),
                    Scenario.domain(decline10), Scenario.domain(decline20));
        }
    }
    public record Scenario(
            @JsonSerialize(using = SrimDecimalSerializer.class) BigDecimal price,
            @JsonSerialize(using = SrimDecimalSerializer.class) BigDecimal differencePercent
    ) {
        static Scenario from(SrimValuation.Scenario s) {
            return s == null ? null : new Scenario(s.price(), s.differencePercent());
        }
        static SrimValuation.Scenario domain(Scenario s) {
            return s == null ? null : new SrimValuation.Scenario(s.price(), s.differencePercent());
        }
    }
}
