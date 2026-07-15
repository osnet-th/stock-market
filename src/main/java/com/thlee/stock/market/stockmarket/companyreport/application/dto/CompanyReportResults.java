package com.thlee.stock.market.stockmarket.companyreport.application.dto;

import com.thlee.stock.market.stockmarket.companyreport.domain.model.InvestmentGrades;
import com.thlee.stock.market.stockmarket.companyreport.domain.model.ReportManual;
import com.thlee.stock.market.stockmarket.companyreport.domain.model.ValuationParams;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 리포트 읽기 유스케이스 출력 묶음
 */
public final class CompanyReportResults {

    private CompanyReportResults() {
    }

    public record ListResult(List<ListItem> items, long totalCount, int page, int size) {}

    public record ListItem(
            Long id,
            String stockCode,
            String stockName,
            InvestmentGrades grades,
            boolean hasBuySignal,
            boolean draft,
            Integer draftStep,
            LocalDateTime snapshotAt,
            LocalDateTime updatedAt
    ) {}

    public record Detail(
            Long id,
            String stockCode,
            String stockName,
            ReportManual manual,
            InvestmentGrades grades,
            boolean hasBuySignal,
            boolean draft,
            Integer draftStep,
            ValuationParams valuationParams,
            ReportSnapshot snapshot,
            ReportValuation valuation,
            LocalDateTime snapshotAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record Preview(ReportSnapshot snapshot, ReportValuation valuation) {}
}
