package com.thlee.stock.market.stockmarket.companyreport.domain.model;

import java.util.stream.Stream;

/**
 * 투자판단 7항목 등급 (전부 수동 입력, 미평가는 null)
 */
public record InvestmentGrades(
        ReportGrade assetUndervalue,
        ReportGrade earningsUndervalue,
        ReportGrade financialHealth,
        ReportGrade profitability,
        ReportGrade growth,
        ReportGrade businessCompetence,
        ReportGrade shareholderPolicy
) {
    public static InvestmentGrades empty() {
        return new InvestmentGrades(null, null, null, null, null, null, null);
    }

    /**
     * 매수재료 여부: 한 항목이라도 A면 true
     */
    public boolean hasBuySignal() {
        return Stream.of(assetUndervalue, earningsUndervalue, financialHealth,
                        profitability, growth, businessCompetence, shareholderPolicy)
                .anyMatch(grade -> grade == ReportGrade.A);
    }
}
