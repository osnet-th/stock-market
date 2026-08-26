package com.thlee.stock.market.stockmarket.companyreport.application.dto;

import com.thlee.stock.market.stockmarket.companyreport.domain.model.InvestmentGrades;
import com.thlee.stock.market.stockmarket.companyreport.domain.model.ReportGrade;
import com.thlee.stock.market.stockmarket.companyreport.domain.model.ReportManual;
import com.thlee.stock.market.stockmarket.companyreport.domain.model.ValuationParams;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
            Map<String, GradeSuggestion> suggestedGrades,
            LocalDateTime snapshotAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record Preview(
            ReportSnapshot snapshot,
            ReportValuation valuation,
            Map<String, GradeSuggestion> suggestedGrades
    ) {}

    /**
     * 정량 항목 제안 등급 (조회 시 파생 계산, 저장 안 함). basis: 판정에 사용한 근거 값 문자열 목록
     */
    public record GradeSuggestion(ReportGrade grade, List<String> basis) {}
}
