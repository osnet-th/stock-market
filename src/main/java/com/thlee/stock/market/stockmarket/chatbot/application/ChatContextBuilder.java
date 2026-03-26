package com.thlee.stock.market.stockmarket.chatbot.application;

import com.thlee.stock.market.stockmarket.chatbot.application.dto.ChatRequest;
import com.thlee.stock.market.stockmarket.portfolio.application.PortfolioAllocationService;
import com.thlee.stock.market.stockmarket.portfolio.application.PortfolioService;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.AllocationResponse;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioItemResponse;
import com.thlee.stock.market.stockmarket.stock.application.StockFinancialService;
import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialIndexResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatContextBuilder {

    private static final String REPORT_CODE_ANNUAL = "11011";
    private static final String INDEX_CLASS_PROFITABILITY = "M210000";
    private static final String INDEX_CLASS_STABILITY = "M220000";

    private final PortfolioService portfolioService;
    private final PortfolioAllocationService portfolioAllocationService;
    private final StockFinancialService stockFinancialService;

    public String build(ChatRequest request) {
        return switch (request.chatMode()) {
            case PORTFOLIO -> buildPortfolioContext(request.userId());
            case FINANCIAL -> buildFinancialContext(request.userId(), request.stockCode());
        };
    }

    private String buildPortfolioContext(Long userId) {
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 주식/투자 전문 금융 어시스턴트입니다. 아래 사용자의 포트폴리오 데이터를 참고하여 자산 배분에 대해 답변하세요.\n\n");

        List<AllocationResponse> allocations = portfolioAllocationService.getAllocation(userId);
        if (!allocations.isEmpty()) {
            sb.append("## 포트폴리오 자산 비중\n");
            allocations.forEach(a ->
                    sb.append("- ").append(a.getAssetTypeName())
                            .append(": ").append(a.getPercentage()).append("%")
                            .append(" (").append(a.getTotalAmount()).append("원)\n")
            );
            sb.append("\n");
        }

        List<PortfolioItemResponse> items = portfolioService.getItems(userId);
        if (!items.isEmpty()) {
            sb.append("## 보유 종목\n");
            items.forEach(item ->
                    sb.append("- ").append(item.getItemName())
                            .append(" (").append(item.getAssetType()).append(", ").append(item.getRegion()).append(")\n")
            );
            sb.append("\n");
        }

        return sb.toString();
    }

    private String buildFinancialContext(Long userId, String stockCode) {
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 주식/투자 전문 금융 어시스턴트입니다. 아래 재무지표 데이터를 참고하여 종목 분석에 대해 답변하세요.\n\n");

        String year = String.valueOf(LocalDate.now().getYear());

        List<FinancialIndexResponse> profitability = stockFinancialService.getFinancialIndices(
                stockCode, year, REPORT_CODE_ANNUAL, INDEX_CLASS_PROFITABILITY);
        if (!profitability.isEmpty()) {
            sb.append("## 수익성 지표\n");
            profitability.forEach(idx ->
                    sb.append("- ").append(idx.getIndexName()).append(": ").append(idx.getIndexValue()).append("\n")
            );
            sb.append("\n");
        }

        List<FinancialIndexResponse> stability = stockFinancialService.getFinancialIndices(
                stockCode, year, REPORT_CODE_ANNUAL, INDEX_CLASS_STABILITY);
        if (!stability.isEmpty()) {
            sb.append("## 안정성 지표\n");
            stability.forEach(idx ->
                    sb.append("- ").append(idx.getIndexName()).append(": ").append(idx.getIndexValue()).append("\n")
            );
            sb.append("\n");
        }

        List<PortfolioItemResponse> items = portfolioService.getItems(userId);
        items.stream()
                .filter(item -> item.getStockDetail() != null && stockCode.equals(item.getStockDetail().getStockCode()))
                .findFirst()
                .ifPresent(item -> {
                    sb.append("## 보유 현황\n");
                    sb.append("- 종목명: ").append(item.getItemName()).append("\n");
                    sb.append("- 보유 수량: ").append(item.getStockDetail().getQuantity()).append("주\n");
                    sb.append("- 매수 평균가: ").append(item.getStockDetail().getAvgBuyPrice()).append("원\n");
                    sb.append("- 투자 금액: ").append(item.getInvestedAmount()).append("원\n");
                    sb.append("\n");
                });

        return sb.toString();
    }
}