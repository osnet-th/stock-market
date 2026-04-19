package com.thlee.stock.market.stockmarket.chatbot.application;

import com.thlee.stock.market.stockmarket.chatbot.application.dto.AnalysisTask;
import com.thlee.stock.market.stockmarket.chatbot.application.dto.ChatRequest;
import com.thlee.stock.market.stockmarket.chatbot.application.dto.FinancialCategory;
import com.thlee.stock.market.stockmarket.chatbot.application.prompt.FinancialAnalysisPromptTemplate;
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
import com.thlee.stock.market.stockmarket.stock.application.ValuationMetricService;
import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialAccountResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialIndexResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.ValuationMetricResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatContextBuilder {

    private static final String REPORT_CODE_ANNUAL = "11011";
    private static final int HISTORY_YEARS = 3;
    private static final String DEFENSIVE_PROMPT = """
            당신은 한국 주식 종목 분석 전문가입니다.
            분석에 필요한 정보(종목 또는 분석 작업)가 누락되었습니다.
            사용자에게 종목을 선택하고 분석 버튼을 눌러달라고 정중히 안내하세요.
            """;

    private final PortfolioService portfolioService;
    private final PortfolioAllocationService portfolioAllocationService;
    private final StockFinancialService stockFinancialService;
    private final ValuationMetricService valuationMetricService;
    private final FinancialAnalysisPromptTemplate promptTemplate;
    private final EcosIndicatorService ecosIndicatorService;
    private final EcosDerivedIndicatorService ecosDerivedIndicatorService;

    public String build(ChatRequest request) {
        return switch (request.chatMode()) {
            case PORTFOLIO -> buildPortfolioContext(request.userId());
            case FINANCIAL -> buildFinancialAnalysis(request);
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

    private String buildFinancialAnalysis(ChatRequest request) {
        if (request.analysisTask() == null || request.stockCode() == null || request.stockCode().isBlank()) {
            return DEFENSIVE_PROMPT;
        }
        String facts = assembleFacts(request.stockCode(), request.analysisTask());
        return promptTemplate.render(request.analysisTask(), facts);
    }

    private String assembleFacts(String stockCode, AnalysisTask task) {
        StringBuilder sb = new StringBuilder();
        int effectiveYear = resolveEffectiveYear(stockCode);

        for (FinancialCategory category : task.categories()) {
            switch (category) {
                case ACCOUNT -> appendAccounts(sb, stockCode, effectiveYear);
                case PROFITABILITY -> appendIndicesAcrossYears(sb, "수익성 지표", stockCode, effectiveYear, "M210000");
                case STABILITY -> appendIndicesAcrossYears(sb, "안정성 지표", stockCode, effectiveYear, "M220000");
                case GROWTH -> appendIndicesAcrossYears(sb, "성장성 지표", stockCode, effectiveYear, "M230000");
                case ACTIVITY -> appendIndicesAcrossYears(sb, "활동성 지표", stockCode, effectiveYear, "M240000");
                case VALUATION -> appendValuation(sb, stockCode);
            }
        }
        return sb.toString();
    }

    private int resolveEffectiveYear(String stockCode) {
        int currentYear = LocalDate.now().getYear();
        try {
            List<FinancialAccountResponse> accounts = stockFinancialService.getFinancialAccounts(
                    stockCode, String.valueOf(currentYear), REPORT_CODE_ANNUAL);
            if (!accounts.isEmpty()) {
                return currentYear;
            }
        } catch (Exception e) {
            log.debug("당해연도 재무계정 조회 실패, 전년도 시도: stockCode={}, year={}", stockCode, currentYear);
        }
        return currentYear - 1;
    }

    private void appendAccounts(StringBuilder sb, String stockCode, int currentYear) {
        List<FinancialAccountResponse> accounts;
        try {
            accounts = stockFinancialService.getFinancialAccounts(
                    stockCode, String.valueOf(currentYear), REPORT_CODE_ANNUAL);
        } catch (Exception e) {
            log.warn("재무계정 조회 실패: stockCode={}, year={}", stockCode, currentYear, e);
            sb.append("## 재무계정\n- 조회 실패: ").append(e.getMessage()).append("\n\n");
            return;
        }
        if (accounts.isEmpty()) {
            sb.append("## 재무계정\n- 데이터 없음\n\n");
            return;
        }

        Map<String, List<FinancialAccountResponse>> grouped = new LinkedHashMap<>();
        for (FinancialAccountResponse account : accounts) {
            grouped.computeIfAbsent(groupLabel(account), k -> new ArrayList<>()).add(account);
        }

        grouped.forEach((group, items) -> {
            sb.append("## 재무계정 (").append(group).append(")\n");
            for (FinancialAccountResponse account : items) {
                sb.append("- ").append(account.getAccountName()).append(": ");
                sb.append(formatYearly(account));
                sb.append("\n");
            }
            sb.append("\n");
        });
    }

    private String groupLabel(FinancialAccountResponse account) {
        if (account.getStatementName() != null && !account.getStatementName().isBlank()) {
            return account.getStatementName();
        }
        if (account.getStatementDiv() != null && !account.getStatementDiv().isBlank()) {
            return account.getStatementDiv();
        }
        return "기타";
    }

    private String formatYearly(FinancialAccountResponse a) {
        StringBuilder sb = new StringBuilder();
        appendTerm(sb, a.getBeforePreviousTermName(), a.getBeforePreviousTermAmount());
        appendTerm(sb, a.getPreviousTermName(), a.getPreviousTermAmount());
        appendTerm(sb, a.getCurrentTermName(), a.getCurrentTermAmount());
        return sb.toString();
    }

    private void appendTerm(StringBuilder sb, String termName, String amount) {
        if (termName == null || termName.isBlank() || amount == null || amount.isBlank()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(" / ");
        }
        sb.append(termName).append("=").append(amount);
    }

    private void appendIndicesAcrossYears(StringBuilder sb, String sectionTitle,
                                          String stockCode, int currentYear, String indexClassCode) {
        List<Integer> years = List.of(currentYear - 2, currentYear - 1, currentYear);

        List<CompletableFuture<YearIndices>> futures = years.stream()
                .map(year -> CompletableFuture.supplyAsync(() -> fetchYearIndices(stockCode, year, indexClassCode)))
                .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        Map<String, Map<Integer, String>> indexToYearlyValues = new LinkedHashMap<>();
        for (CompletableFuture<YearIndices> f : futures) {
            YearIndices y = f.join();
            for (FinancialIndexResponse idx : y.indices()) {
                indexToYearlyValues
                        .computeIfAbsent(idx.getIndexName(), k -> new LinkedHashMap<>())
                        .put(y.year(), idx.getIndexValue());
            }
        }

        sb.append("## ").append(sectionTitle).append("\n");
        if (indexToYearlyValues.isEmpty()) {
            sb.append("- 데이터 없음\n\n");
            return;
        }
        indexToYearlyValues.forEach((indexName, yearly) -> {
            sb.append("- ").append(indexName).append(": ");
            boolean first = true;
            for (Integer year : years) {
                String value = yearly.get(year);
                if (value == null) {
                    continue;
                }
                if (!first) {
                    sb.append(", ");
                }
                sb.append(year).append("=").append(value);
                first = false;
            }
            sb.append("\n");
        });
        sb.append("\n");
    }

    private YearIndices fetchYearIndices(String stockCode, int year, String indexClassCode) {
        try {
            List<FinancialIndexResponse> list = stockFinancialService.getFinancialIndices(
                    stockCode, String.valueOf(year), REPORT_CODE_ANNUAL, indexClassCode);
            return new YearIndices(year, list);
        } catch (Exception e) {
            log.warn("지표 조회 실패: stockCode={}, year={}, indexClass={}", stockCode, year, indexClassCode, e);
            return new YearIndices(year, List.of());
        }
    }

    private void appendValuation(StringBuilder sb, String stockCode) {
        ValuationMetricResponse response;
        try {
            response = valuationMetricService.calculate(stockCode);
        } catch (Exception e) {
            log.warn("가치평가 지표 계산 실패: stockCode={}", stockCode, e);
            sb.append("## 가치평가 지표\n- 계산 실패: ").append(e.getMessage()).append("\n\n");
            return;
        }

        sb.append("## 가치평가 지표");
        if (response.getTermName() != null) {
            sb.append(" (기준 회계기: ").append(response.getTermName());
            if (response.getReferencePriceDate() != null) {
                sb.append(", 기준 주가일: ").append(response.getReferencePriceDate());
            }
            sb.append(")");
        }
        sb.append("\n");

        appendValuationLine(sb, "EPS", response.getEps(), "원");
        appendValuationLine(sb, "BPS", response.getBps(), "원");
        appendValuationLine(sb, "PER", response.getPer(), "배");
        appendValuationLine(sb, "PBR", response.getPbr(), "배");
        if (response.getReferencePrice() != null) {
            sb.append("- 기준 주가: ").append(response.getReferencePrice().toPlainString()).append("원\n");
        }
        if (response.getWarnings() != null) {
            for (String warning : response.getWarnings()) {
                sb.append("- 참고: ").append(warning).append("\n");
            }
        }
        sb.append("- 참고: 역사 주가 API 부재로 과거 PER/PBR은 제공되지 않음. 추세 판단 시 재무계정·수익성 지표 3개년을 활용하세요.\n\n");
    }

    private void appendValuationLine(StringBuilder sb, String label, BigDecimal value, String unit) {
        sb.append("- ").append(label).append(": ");
        if (value == null) {
            sb.append("N/A");
        } else {
            sb.append(value.toPlainString()).append(unit);
        }
        sb.append("\n");
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

    private record YearIndices(int year, List<FinancialIndexResponse> indices) {}
}
