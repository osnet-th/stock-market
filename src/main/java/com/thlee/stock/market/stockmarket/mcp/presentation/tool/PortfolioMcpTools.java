package com.thlee.stock.market.stockmarket.mcp.presentation.tool;

import com.thlee.stock.market.stockmarket.mcp.application.McpPortfolioQueryService;
import com.thlee.stock.market.stockmarket.mcp.application.dto.McpHoldings;
import com.thlee.stock.market.stockmarket.mcp.presentation.McpUserContext;
import com.thlee.stock.market.stockmarket.mcp.presentation.dto.McpDividendSchedule;
import com.thlee.stock.market.stockmarket.mcp.presentation.dto.McpPortfolioSummary;
import com.thlee.stock.market.stockmarket.portfolio.application.PortfolioSummaryService;
import com.thlee.stock.market.stockmarket.stockevaluation.application.StockEvaluationService;
import com.thlee.stock.market.stockmarket.stockevaluation.domain.model.KsdScheduleType;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 포트폴리오·배당 일정 MCP 도구 (조회 전용)
 */
@Component
@RequiredArgsConstructor
public class PortfolioMcpTools {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
    /** 화면 배당 일정과 같은 기본 기간: 6개월 전 ~ 3개월 후 */
    private static final int DEFAULT_FROM_MONTHS = 6;
    private static final int DEFAULT_TO_MONTHS = 3;

    private final McpPortfolioQueryService portfolioQueryService;
    private final PortfolioSummaryService portfolioSummaryService;
    private final StockEvaluationService stockEvaluationService;

    @McpTool(name = "get_my_holdings", title = "내 보유 종목",
            annotations = @McpTool.McpAnnotations(title = "내 보유 종목", readOnlyHint = true,
                    destructiveHint = false, idempotentHint = true, openWorldHint = true),
            description = "내 포트폴리오의 보유 주식(전량 매도 제외)과 현재가를 조회한다. "
                    + "평가액·손익은 수량 × 원화 현재가 기준(원)이고, 현재가를 못 가져온 종목은 평가액이 비고 priceNote에 이유가 있다. "
                    + "주식 외 자산과 전체 합계는 get_my_portfolio_summary를 쓴다.")
    public McpHoldings getMyHoldings() {
        return portfolioQueryService.holdings(McpUserContext.currentUserId());
    }

    @McpTool(name = "get_my_portfolio_summary", title = "내 포트폴리오 요약",
            annotations = @McpTool.McpAnnotations(title = "내 포트폴리오 요약", readOnlyHint = true,
                    destructiveHint = false, idempotentHint = true, openWorldHint = true),
            description = "내 포트폴리오 전체(주식·현금·채권·펀드 등) 평가액·원금·손익·수익률·보유일·CAGR·환차익과 "
                    + "이번 달/연간 예상 배당·이자를 조회한다. 금액은 원, 비율은 %.")
    public McpPortfolioSummary getMyPortfolioSummary() {
        return McpPortfolioSummary.from(portfolioSummaryService.getSummary(McpUserContext.currentUserId()));
    }

    @McpTool(name = "get_dividend_schedule", title = "종목 배당 일정",
            annotations = @McpTool.McpAnnotations(title = "종목 배당 일정", readOnlyHint = true,
                    destructiveHint = false, idempotentHint = true, openWorldHint = true),
            description = "국내 종목의 배당 일정(기준일·현금배당금·배당률·지급일 등)을 예탁원 자료로 조회한다. "
                    + "기간 생략 시 6개월 전 ~ 3개월 후.")
    public McpDividendSchedule getDividendSchedule(
            @McpToolParam(description = "국내 종목코드 6자리 (예: 005930)") String stockCode,
            @McpToolParam(description = "시작일 YYYY-MM-DD. 생략하면 6개월 전", required = false) String fromDate,
            @McpToolParam(description = "종료일 YYYY-MM-DD. 생략하면 3개월 후", required = false) String toDate) {
        McpUserContext.currentUserId();
        if (stockCode == null || !stockCode.strip().matches("^\\d{6}$")) {
            throw new IllegalArgumentException("국내 종목코드 6자리가 필요합니다.");
        }
        LocalDate today = LocalDate.now(KST);
        LocalDate from = StringUtils.hasText(fromDate) ? LocalDate.parse(fromDate.strip())
                : today.minusMonths(DEFAULT_FROM_MONTHS);
        LocalDate to = StringUtils.hasText(toDate) ? LocalDate.parse(toDate.strip())
                : today.plusMonths(DEFAULT_TO_MONTHS);
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("시작일이 종료일보다 늦습니다.");
        }
        String code = stockCode.strip();
        return McpDividendSchedule.from(code, from, to, stockEvaluationService.getSchedule(
                code, KsdScheduleType.DIVIDEND, from.format(YMD), to.format(YMD)));
    }
}
