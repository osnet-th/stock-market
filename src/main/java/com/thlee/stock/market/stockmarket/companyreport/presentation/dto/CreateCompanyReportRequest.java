package com.thlee.stock.market.stockmarket.companyreport.presentation.dto;

import com.thlee.stock.market.stockmarket.companyreport.application.dto.CompanyReportCommands;
import com.thlee.stock.market.stockmarket.companyreport.domain.model.InvestmentGrades;
import com.thlee.stock.market.stockmarket.companyreport.domain.model.ReportGrade;
import com.thlee.stock.market.stockmarket.companyreport.domain.model.ReportManual;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 리포트 생성 요청. manual은 항목별 구조화 입력(길이/개수 상한은 도메인에서 검증).
 */
public record CreateCompanyReportRequest(
        @NotBlank @Pattern(regexp = "\\d{6}", message = "종목코드는 6자리 숫자여야 합니다.")
        String stockCode,
        ReportManual manual,
        String gradeAssetUndervalue,
        String gradeEarningsUndervalue,
        String gradeFinancialHealth,
        String gradeProfitability,
        String gradeGrowth,
        String gradeBusinessCompetence,
        String gradeShareholderPolicy,
        ValuationParamsRequest valuationParams,
        Boolean draft,
        Integer draftStep
) {

    public CompanyReportCommands.Create toCommand(Long userId) {
        return new CompanyReportCommands.Create(
                userId, stockCode, manual, toGrades(), ValuationParamsRequest.toParams(valuationParams),
                Boolean.TRUE.equals(draft), draftStep);
    }

    private InvestmentGrades toGrades() {
        return new InvestmentGrades(
                ReportGrade.fromNullable(gradeAssetUndervalue),
                ReportGrade.fromNullable(gradeEarningsUndervalue),
                ReportGrade.fromNullable(gradeFinancialHealth),
                ReportGrade.fromNullable(gradeProfitability),
                ReportGrade.fromNullable(gradeGrowth),
                ReportGrade.fromNullable(gradeBusinessCompetence),
                ReportGrade.fromNullable(gradeShareholderPolicy));
    }
}
