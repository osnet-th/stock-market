package com.thlee.stock.market.stockmarket.stock.application.dto;

import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialTimelineResponse.TimelineColumn;
import com.thlee.stock.market.stockmarket.stock.domain.model.FinancialAccount;
import com.thlee.stock.market.stockmarket.stock.domain.model.FullFinancialStatement;
import com.thlee.stock.market.stockmarket.stock.domain.model.StockQuantity;

import java.util.List;

/**
 * 타임라인 컬럼 1개(연도)의 수집 데이터 (조립 전 내부 캐리어)
 */
public record TimelineColumnData(
        TimelineColumn column,
        List<FinancialAccount> accounts,
        List<FinancialIndexResponse> indices,
        List<StockQuantity> shares,
        List<FullFinancialStatement> details
) {
    public String year() {
        return column.getYear();
    }
}
