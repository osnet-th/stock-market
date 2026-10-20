package com.thlee.stock.market.stockmarket.mcp.application;

import com.thlee.stock.market.stockmarket.mcp.application.dto.McpHoldings;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioItemResponse;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.StockDetailResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 보유 종목 + 현재가 → Claude 응답. 평가액은 수량 × 원화 현재가다(화면 평가와 같은 기준).
 * 원화 원금은 기록된 원화 매수 금액을 쓰고, 해외 종목에 기록이 없으면 화면처럼 원금 × 현재 환율로 환산한다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class McpHoldingsMapper {

    static final String KRW = "KRW";
    static final String PRICE_MISSING = "현재가를 가져오지 못했습니다(장애·미지원 종목). 평가액·손익을 비웠습니다.";
    static final String PRINCIPAL_MISSING = "원화 원금을 알 수 없어(매수 환율 기록·현재 환율 없음) 원금·손익을 비웠습니다.";

    /** 종목별 현재가. 가격 값은 시세 응답의 문자열 그대로, exchangeRate는 1 외화당 원 */
    public record Quote(String currentPrice, String currency, String currentPriceKrw, BigDecimal exchangeRate,
                        String changeRate, String cachedAt) {}

    public static McpHoldings toHoldings(List<PortfolioItemResponse> stockItems, Map<String, Quote> quotes) {
        List<McpHoldings.Holding> holdings = stockItems.stream()
                .map(item -> toHolding(item, quotes.get(item.getStockDetail().getStockCode())))
                .toList();
        int missing = (int) holdings.stream().filter(h -> h.priceNote() != null).count();
        return new McpHoldings(KRW, holdings, missing);
    }

    private static McpHoldings.Holding toHolding(PortfolioItemResponse item, Quote quote) {
        StockDetailResponse d = item.getStockDetail();
        BigDecimal invested = investedKrw(item, quote);
        BigDecimal evaluated = evaluatedKrw(d, quote);
        BigDecimal profit = evaluated != null && invested != null ? evaluated.subtract(invested) : null;
        List<String> notes = new ArrayList<>();
        if (evaluated == null) {
            notes.add(PRICE_MISSING);
        }
        if (invested == null) {
            notes.add(PRINCIPAL_MISSING);
        }
        return new McpHoldings.Holding(
                item.getId(), item.getItemName(), d.getStockCode(), d.getMarket(), d.getCountry(),
                item.getInstitution(), d.getQuantity(), d.getAvgBuyPrice(), d.getPriceCurrency(), invested,
                quote == null ? null : quote.currentPrice(),
                quote == null ? null : quote.currency(),
                quote == null ? null : quote.currentPriceKrw(),
                quote == null ? null : quote.changeRate(),
                quote == null ? null : quote.cachedAt(),
                evaluated, profit, d.getDividendYield(),
                notes.isEmpty() ? null : String.join(" ", notes));
    }

    /** 시세가 비면 원화 현재가가 "0"으로 온다 — 없는 값으로 본다 */
    private static BigDecimal evaluatedKrw(StockDetailResponse d, Quote quote) {
        if (quote == null || quote.currentPriceKrw() == null || d.getQuantity() == null) {
            return null;
        }
        BigDecimal priceKrw = new BigDecimal(quote.currentPriceKrw());
        return priceKrw.signum() > 0 ? priceKrw.multiply(BigDecimal.valueOf(d.getQuantity())) : null;
    }

    private static BigDecimal investedKrw(PortfolioItemResponse item, Quote quote) {
        StockDetailResponse d = item.getStockDetail();
        if (d.getInvestedAmountKrw() != null) {
            return d.getInvestedAmountKrw();
        }
        if (d.getPriceCurrency() == null || KRW.equals(d.getPriceCurrency())) {
            return item.getInvestedAmount();
        }
        if (quote == null || quote.exchangeRate() == null || quote.exchangeRate().signum() <= 0
                || item.getInvestedAmount() == null) {
            return null;
        }
        return item.getInvestedAmount().multiply(quote.exchangeRate());
    }
}
