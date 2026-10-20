package com.thlee.stock.market.stockmarket.mcp.presentation.dto;

import com.thlee.stock.market.stockmarket.stockevaluation.application.dto.KisTableResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * 종목 배당 일정 (예탁원 자료, 한국투자증권 경유). 표 형태이며 rows는 columns 순서의 값이다.
 * 현금배당금은 1주당 원, 배당률은 %, 날짜는 YYYYMMDD 문자열이다.
 */
public record McpDividendSchedule(String stockCode, LocalDate fromDate, LocalDate toDate, String source,
                                  List<String> columns, List<List<String>> rows) {

    public static McpDividendSchedule from(String stockCode, LocalDate from, LocalDate to, KisTableResponse table) {
        return new McpDividendSchedule(stockCode, from, to, "예탁원 배당일정(한국투자증권)",
                table.getColumns(), table.getRows());
    }
}
