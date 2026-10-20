package com.thlee.stock.market.stockmarket.mcp.application.dto;

import com.thlee.stock.market.stockmarket.companyreport.application.dto.SrimData;
import com.thlee.stock.market.stockmarket.companyreport.domain.model.InvestmentGrades;
import com.thlee.stock.market.stockmarket.companyreport.domain.model.ReportManual;

import java.time.LocalDateTime;

/**
 * 내가 저장한 기업 리포트 상세 (Claude 응답용).
 * summary는 저장 당시 스냅샷 기준이고, srim은 저장 당시 계산 결과다(요구수익률·ROE는 소수).
 */
public record McpCompanyReportDetail(
        Long id,
        String stockCode,
        String stockName,
        InvestmentGrades grades,
        boolean hasBuySignal,
        boolean draft,
        ReportManual manual,
        McpCompanyReportSummary summary,
        SrimData srim,
        LocalDateTime snapshotAt,
        LocalDateTime updatedAt
) {}
