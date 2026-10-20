package com.thlee.stock.market.stockmarket.mcp.presentation.tool;

import com.thlee.stock.market.stockmarket.economics.application.BondYieldQueryService;
import com.thlee.stock.market.stockmarket.economics.application.EcosIndicatorService;
import com.thlee.stock.market.stockmarket.economics.application.GlobalIndicatorQueryService;
import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorCategory;
import com.thlee.stock.market.stockmarket.economics.domain.model.IndicatorCategory;
import com.thlee.stock.market.stockmarket.mcp.presentation.McpUserContext;
import com.thlee.stock.market.stockmarket.mcp.presentation.dto.McpBondYields;
import com.thlee.stock.market.stockmarket.mcp.presentation.dto.McpEconomicIndicators;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 경제지표·채권 금리 MCP 도구 (조회 전용)
 */
@Component
@RequiredArgsConstructor
public class EconomicsMcpTools {

    private final BondYieldQueryService bondYieldQueryService;
    private final EcosIndicatorService ecosIndicatorService;
    private final GlobalIndicatorQueryService globalIndicatorQueryService;

    @McpTool(name = "get_bond_yields", title = "채권 기준수익률",
            annotations = @McpTool.McpAnnotations(title = "채권 기준수익률", readOnlyHint = true,
                    destructiveHint = false, idempotentHint = true, openWorldHint = true),
            description = "날짜별 채권 기준수익률(국고채·회사채 AAA~BBB- 만기별, % 단위)을 조회한다. "
                    + "S-RIM 요구수익률을 고를 때 쓴다. 요청일 값이 없으면 최대 10일 전 값을 쓰고 status=FALLBACK으로 알린다.")
    public McpBondYields getBondYields(
            @McpToolParam(description = "기준일 YYYY-MM-DD. 생략하면 오늘(KST)", required = false) String date) {
        McpUserContext.currentUserId();
        LocalDate requested = StringUtils.hasText(date) ? LocalDate.parse(date.strip()) : null;
        return McpBondYields.from(bondYieldQueryService.lookup(requested));
    }

    @McpTool(name = "get_economic_indicators", title = "경제지표",
            annotations = @McpTool.McpAnnotations(title = "경제지표", readOnlyHint = true,
                    destructiveHint = false, idempotentHint = true, openWorldHint = true),
            description = "경제지표를 조회한다. category 생략 시 최근 7일 안에 값이 바뀐 국내(한국은행 ECOS)·해외 지표. "
                    + "국내 카테고리: " + "INTEREST_RATE(금리), MONEY_FINANCE(통화/금융), STOCK_BOND(주식/채권), EXCHANGE_RATE(환율), "
                    + "GROWTH_INCOME(성장/소득), PRODUCTION(생산), CONSUMPTION_INVESTMENT(소비/투자), PRICE(물가), "
                    + "EMPLOYMENT_LABOR(고용/노동), SENTIMENT(경기심리), EXTERNAL_ECONOMY(대외경제), "
                    + "CORPORATE_HOUSEHOLD(기업/가계), REAL_ESTATE(부동산), POPULATION(인구), COMMODITY(원자재). "
                    + "해외(국가별) 카테고리: TRADE_GDP(무역/GDP), EMPLOYMENT(고용), PRICE_CONSUMPTION(물가/소비), "
                    + "RATE_FINANCE(금리/금융), CONFIDENCE_LEADING(신뢰/선행 지수), PMI_INDUSTRY(PMI/산업), "
                    + "FISCAL_STATE(재정/국가), MONEY_SUPPLY(통화량). 값·단위는 출처 문자열 그대로다.")
    public McpEconomicIndicators getEconomicIndicators(
            @McpToolParam(description = "카테고리 코드 또는 이름(예: INTEREST_RATE, 금리, RATE_FINANCE). 생략하면 최근 갱신 지표",
                    required = false) String category) {
        McpUserContext.currentUserId();
        if (!StringUtils.hasText(category)) {
            return McpEconomicIndicators.recent(ecosIndicatorService.findRecentlyUpdated(),
                    globalIndicatorQueryService.findRecentlyUpdated());
        }
        String key = category.strip();
        Optional<EcosIndicatorCategory> ecos = Arrays.stream(EcosIndicatorCategory.values())
                .filter(c -> c.name().equalsIgnoreCase(key) || c.getLabel().equals(key))
                .findFirst();
        if (ecos.isPresent()) {
            return McpEconomicIndicators.ecos(ecos.get().name(),
                    ecosIndicatorService.getIndicatorsByCategory(ecos.get()));
        }
        Optional<IndicatorCategory> global = Arrays.stream(IndicatorCategory.values())
                .filter(c -> c.name().equalsIgnoreCase(key) || c.getDisplayName().equals(key))
                .findFirst();
        if (global.isPresent()) {
            return McpEconomicIndicators.global(global.get().name(),
                    globalIndicatorQueryService.getIndicatorsByCategory(global.get()));
        }
        throw new IllegalArgumentException("알 수 없는 카테고리입니다: " + key + ". 사용 가능: " + Stream.concat(
                Arrays.stream(EcosIndicatorCategory.values()).map(Enum::name),
                Arrays.stream(IndicatorCategory.values()).map(Enum::name)).collect(Collectors.joining(", ")));
    }
}
