package com.thlee.stock.market.stockmarket.companyreport.application.dto;

import com.thlee.stock.market.stockmarket.companyreport.domain.model.InvestmentGrades;
import com.thlee.stock.market.stockmarket.companyreport.domain.model.ReportManual;
import com.thlee.stock.market.stockmarket.companyreport.domain.model.ValuationParams;

/**
 * 리포트 쓰기 유스케이스 입력 묶음
 */
public final class CompanyReportCommands {

    private CompanyReportCommands() {
    }

    public record Create(
            Long userId,
            String stockCode,
            ReportManual manual,
            InvestmentGrades grades,
            ValuationParams params,
            boolean draft,
            Integer draftStep
    ) {}

    public record Update(
            Long id,
            Long userId,
            ReportManual manual,
            InvestmentGrades grades,
            ValuationParams params,
            boolean draft,
            Integer draftStep
    ) {}
}
