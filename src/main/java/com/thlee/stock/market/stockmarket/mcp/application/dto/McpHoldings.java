package com.thlee.stock.market.stockmarket.mcp.application.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 내 보유 종목과 현재가 (Claude 응답용). *Krw 금액은 원 단위다.
 *
 * @param priceMissingCount 현재가를 가져오지 못해 평가액이 빈 종목 수
 */
public record McpHoldings(String currency, List<Holding> items, int priceMissingCount) {

    /**
     * 보유 종목 1건.
     *
     * @param avgBuyPrice      평균 매수가 (priceCurrency 단위)
     * @param currentPrice     현재가 (currentPriceCurrency 단위)
     * @param changeRatePercent 전일 대비 등락률(%)
     * @param priceCachedAt    현재가 조회(캐시) 시각
     * @param priceNote        현재가가 없을 때 이유. 이때 평가액·손익은 비어 있다
     */
    public record Holding(
            Long id,
            String itemName,
            String stockCode,
            String market,
            String country,
            String institution,
            Integer quantity,
            BigDecimal avgBuyPrice,
            String priceCurrency,
            BigDecimal investedAmountKrw,
            String currentPrice,
            String currentPriceCurrency,
            String currentPriceKrw,
            String changeRatePercent,
            String priceCachedAt,
            BigDecimal evaluatedAmountKrw,
            BigDecimal profitKrw,
            BigDecimal dividendYieldPercent,
            String priceNote
    ) {}
}
