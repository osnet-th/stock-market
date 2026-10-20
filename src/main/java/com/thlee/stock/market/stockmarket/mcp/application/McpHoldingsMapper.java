package com.thlee.stock.market.stockmarket.mcp.application;

import com.thlee.stock.market.stockmarket.mcp.application.dto.McpHoldings;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioItemResponse;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.StockDetailResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 보유 종목 + 현재가 → Claude 응답. 평가액은 수량 × 원화 현재가다(화면 평가와 같은 기준).
 */
public final class McpHoldingsMapper {

    static final String PRICE_MISSING = "현재가를 가져오지 못했습니다(장애·미지원 종목). 평가액·손익을 비웠습니다.";

    private McpHoldingsMapper() {
    }

    /** 종목별 현재가. 값은 시세 응답의 문자열 그대로다 */
    public record Quote(String currentPrice, String currency, String currentPriceKrw, String changeRate,
                        String cachedAt) {}

    public static McpHoldings toHoldings(List<PortfolioItemResponse> stockItems, Map<String, Quote> quotes) {
        List<McpHoldings.Holding> holdings = stockItems.stream()
                .map(item -> toHolding(item, quotes.get(item.getStockDetail().getStockCode())))
                .toList();
        int missing = (int) holdings.stream().filter(h -> h.priceNote() != null).count();
        return new McpHoldings("KRW", holdings, missing);
    }

    private static McpHoldings.Holding toHolding(PortfolioItemResponse item, Quote quote) {
        StockDetailResponse d = item.getStockDetail();
        BigDecimal invested = d.getInvestedAmountKrw() != null ? d.getInvestedAmountKrw() : item.getInvestedAmount();
        BigDecimal priceKrw = quote == null || quote.currentPriceKrw() == null ? null
                : new BigDecimal(quote.currentPriceKrw());
        // 시세가 비면 원화 현재가가 "0"으로 온다 — 없는 값으로 본다
        boolean priced = priceKrw != null && priceKrw.signum() > 0 && d.getQuantity() != null;
        BigDecimal evaluated = priced ? priceKrw.multiply(BigDecimal.valueOf(d.getQuantity())) : null;
        BigDecimal profit = evaluated != null && invested != null ? evaluated.subtract(invested) : null;
        return new McpHoldings.Holding(
                item.getId(), item.getItemName(), d.getStockCode(), d.getMarket(), d.getCountry(),
                item.getInstitution(), d.getQuantity(), d.getAvgBuyPrice(), d.getPriceCurrency(), invested,
                quote == null ? null : quote.currentPrice(),
                quote == null ? null : quote.currency(),
                quote == null ? null : quote.currentPriceKrw(),
                quote == null ? null : quote.changeRate(),
                quote == null ? null : quote.cachedAt(),
                evaluated, profit, d.getDividendYield(),
                priced ? null : PRICE_MISSING);
    }
}
