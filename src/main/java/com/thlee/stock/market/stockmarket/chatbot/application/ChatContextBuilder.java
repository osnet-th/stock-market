package com.thlee.stock.market.stockmarket.chatbot.application;

import com.thlee.stock.market.stockmarket.chatbot.application.dto.ChatRequest;
import com.thlee.stock.market.stockmarket.economics.application.EcosDerivedIndicatorService;
import com.thlee.stock.market.stockmarket.economics.application.EcosIndicatorService;
import com.thlee.stock.market.stockmarket.economics.domain.model.DerivedIndicator;
import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorCategory;
import com.thlee.stock.market.stockmarket.economics.domain.model.KeyStatIndicator;
import com.thlee.stock.market.stockmarket.portfolio.application.PortfolioAllocationService;
import com.thlee.stock.market.stockmarket.portfolio.application.PortfolioService;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.AllocationResponse;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioItemResponse;
import com.thlee.stock.market.stockmarket.stock.application.StockFinancialService;
import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialIndexResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatContextBuilder {

    private static final String REPORT_CODE_ANNUAL = "11011";
    private static final String INDEX_CLASS_PROFITABILITY = "M210000";
    private static final String INDEX_CLASS_STABILITY = "M220000";

    private final PortfolioService portfolioService;
    private final PortfolioAllocationService portfolioAllocationService;
    private final StockFinancialService stockFinancialService;
    private final EcosIndicatorService ecosIndicatorService;
    private final EcosDerivedIndicatorService ecosDerivedIndicatorService;

    public String build(ChatRequest request) {
        return switch (request.chatMode()) {
            case PORTFOLIO -> buildPortfolioContext(request.userId());
            case FINANCIAL -> buildFinancialContext(request.userId(), request.stockCode());
            case ECONOMIC -> buildEconomicContext(request.indicatorCategory());
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

    private String buildEconomicContext(String indicatorCategory) {
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 한국 경제지표 전문 분석 어시스턴트입니다.\n");
        sb.append("아래 제공된 경제지표 데이터를 기반으로 현재 경제 상황을 분석하고 해석해주세요.\n");
        sb.append("각 지표의 기준일(cycle)을 참고하여, 데이터 시점을 언급해주세요.\n");
        sb.append("파생지표가 포함된 경우 해당 값이 의미하는 바를 구체적으로 설명해주세요.\n\n");

        try {
            EcosIndicatorCategory category = EcosIndicatorCategory.valueOf(indicatorCategory);
            List<KeyStatIndicator> indicators = ecosIndicatorService.getIndicatorsByCategory(category);

            if (indicators.isEmpty()) {
                sb.append("현재 해당 카테고리의 경제지표 데이터가 없습니다.\n");
                return sb.toString();
            }

            sb.append("## 원시 경제지표 (").append(category.getLabel()).append(")\n");
            for (KeyStatIndicator ind : indicators) {
                sb.append("- ").append(ind.keystatName());
                sb.append(": ").append(ind.dataValue());
                if (ind.unitName() != null && !ind.unitName().isBlank()) {
                    sb.append(" (").append(ind.unitName()).append(")");
                }
                if (ind.cycle() != null && !ind.cycle().isBlank()) {
                    sb.append(" [기준: ").append(ind.cycle()).append("]");
                }
                sb.append("\n");
            }
            sb.append("\n");

            List<DerivedIndicator> derived = ecosDerivedIndicatorService.calculate(category, indicators);
            if (!derived.isEmpty()) {
                sb.append("## 파생지표\n");
                for (DerivedIndicator d : derived) {
                    sb.append("- ").append(d.name());
                    sb.append(": ").append(d.value());
                    if (d.unit() != null && !d.unit().isBlank()) {
                        sb.append(" (").append(d.unit()).append(")");
                    }
                    sb.append(" — ").append(d.description());
                    sb.append("\n");
                }
                sb.append("\n");
            }

        } catch (IllegalArgumentException e) {
            log.warn("잘못된 경제지표 카테고리: {}", indicatorCategory);
            sb.append("잘못된 카테고리입니다. 유효한 경제지표 카테고리를 선택해주세요.\n");
        } catch (Exception e) {
            log.error("경제지표 조회 실패: category={}", indicatorCategory, e);
            sb.append("현재 경제지표 데이터를 조회할 수 없습니다. 잠시 후 다시 시도해주세요.\n");
        }

        return sb.toString();
    }
}