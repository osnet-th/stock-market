package com.thlee.stock.market.stockmarket.mcp.presentation.tool;

import com.thlee.stock.market.stockmarket.economics.application.BondYieldQueryService;
import com.thlee.stock.market.stockmarket.mcp.presentation.McpUserContext;
import com.thlee.stock.market.stockmarket.mcp.presentation.dto.McpBondYields;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

/**
 * 경제지표·채권 금리 MCP 도구 (조회 전용)
 */
@Component
@RequiredArgsConstructor
public class EconomicsMcpTools {

    private final BondYieldQueryService bondYieldQueryService;

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
}
