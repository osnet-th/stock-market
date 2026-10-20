package com.thlee.stock.market.stockmarket.mcp.presentation.tool;

import com.thlee.stock.market.stockmarket.companyreport.application.dto.CompanyReportResults;
import com.thlee.stock.market.stockmarket.mcp.application.McpCompanyReportQueryService;
import com.thlee.stock.market.stockmarket.mcp.application.dto.McpCompanyReportDetail;
import com.thlee.stock.market.stockmarket.mcp.application.dto.McpCompanyReportSummary;
import com.thlee.stock.market.stockmarket.mcp.application.dto.McpSrimResult;
import com.thlee.stock.market.stockmarket.mcp.presentation.McpUserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * 기업 리포트·S-RIM MCP 도구 (조회 전용)
 */
@Component
@RequiredArgsConstructor
public class CompanyReportMcpTools {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final McpCompanyReportQueryService queryService;

    @McpTool(name = "get_company_report_summary", title = "기업 리포트 요약",
            annotations = @McpTool.McpAnnotations(title = "기업 리포트 요약", readOnlyHint = true,
                    destructiveHint = false, idempotentHint = true, openWorldHint = true),
            description = "종목코드로 기업 리포트 요약을 만든다(DART·KIS 실시간 조회, 저장하지 않음). "
                    + "주가지표(PER·PBR 등, 주가 기준일), 최근 5개 연도 비율(%), 위험 신호, 청산가치·DCF(기본 가정)를 담는다. "
                    + "금액은 currency의 기본 단위(원·달러)이고 baseYear는 최근 사업보고서 연도다.")
    public McpCompanyReportSummary getCompanyReportSummary(
            @McpToolParam(description = "종목코드. 국내 6자리(예: 005930) 또는 미국 티커(예: AAPL)") String stockCode) {
        McpUserContext.currentUserId();
        return queryService.summary(requireStockCode(stockCode));
    }

    @McpTool(name = "list_my_company_reports", title = "내 기업 리포트 목록",
            annotations = @McpTool.McpAnnotations(title = "내 기업 리포트 목록", readOnlyHint = true,
                    destructiveHint = false, idempotentHint = true, openWorldHint = false),
            description = "내가 저장한 기업 리포트 목록(id, 종목, 투자판단 등급, 매수재료 여부, 임시저장 여부, 갱신 시각)을 조회한다. "
                    + "상세는 get_my_company_report에 id를 넣어 본다.")
    public CompanyReportResults.ListResult listMyCompanyReports(
            @McpToolParam(description = "종목명 검색어. 생략하면 전체", required = false) String stockName,
            @McpToolParam(description = "0부터 시작하는 페이지 번호. 기본 0", required = false) Integer page,
            @McpToolParam(description = "페이지 크기(1~50). 기본 20", required = false) Integer size) {
        Long userId = McpUserContext.currentUserId();
        return queryService.list(userId, StringUtils.hasText(stockName) ? stockName.strip() : null,
                page == null ? 0 : page, size == null ? DEFAULT_PAGE_SIZE : size);
    }

    @McpTool(name = "get_my_company_report", title = "내 기업 리포트 상세",
            annotations = @McpTool.McpAnnotations(title = "내 기업 리포트 상세", readOnlyHint = true,
                    destructiveHint = false, idempotentHint = true, openWorldHint = false),
            description = "내가 저장한 기업 리포트 1건을 조회한다. 투자판단 등급, 정성 메모(manual), "
                    + "저장 당시 스냅샷 요약, 저장된 S-RIM 결과(요구수익률·ROE는 소수)를 담는다. 다른 사용자의 리포트는 조회되지 않는다.")
    public McpCompanyReportDetail getMyCompanyReport(
            @McpToolParam(description = "리포트 id (list_my_company_reports 결과)") Long reportId) {
        Long userId = McpUserContext.currentUserId();
        if (reportId == null) {
            throw new IllegalArgumentException("reportId가 필요합니다.");
        }
        return queryService.detail(reportId, userId);
    }

    @McpTool(name = "calculate_srim", title = "S-RIM 적정주가",
            annotations = @McpTool.McpAnnotations(title = "S-RIM 적정주가", readOnlyHint = true,
                    destructiveHint = false, idempotentHint = true, openWorldHint = true),
            description = "종목코드로 S-RIM 적정주가(초과이익 영구 지속·매년 10% 감소·20% 감소)와 기준 주가 대비 차이(%)를 계산한다(저장하지 않음). "
                    + "자기자본·주식수는 국내 연결재무제표 근거를 쓰며 없으면 calculable=false와 이유를 돌려준다. "
                    + "요구수익률 생략 시 오늘 회사채(공모 무보증) BBB- 5년 금리, ROE 생략 시 리포트 최근 연간 ROE를 쓰고 출처를 응답에 담는다.")
    public McpSrimResult calculateSrim(
            @McpToolParam(description = "종목코드. 국내 6자리(예: 005930)") String stockCode,
            @McpToolParam(description = "요구수익률(%), 예: 8.5. 생략하면 회사채 BBB- 5년 금리", required = false)
            BigDecimal requiredReturnPercent,
            @McpToolParam(description = "예상 ROE(%), 예: 12. 생략하면 리포트 최근 연간 ROE", required = false)
            BigDecimal roePercent) {
        McpUserContext.currentUserId();
        return queryService.srim(requireStockCode(stockCode), requiredReturnPercent, roePercent);
    }

    private static String requireStockCode(String stockCode) {
        if (!StringUtils.hasText(stockCode)) {
            throw new IllegalArgumentException("stockCode가 필요합니다.");
        }
        return stockCode.strip().toUpperCase();
    }
}
