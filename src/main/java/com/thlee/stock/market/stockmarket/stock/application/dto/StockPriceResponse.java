package com.thlee.stock.market.stockmarket.stock.application.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.StockPrice;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.config.StockPriceCacheConfig;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

@Getter
@RequiredArgsConstructor
public class StockPriceResponse {

    private final String stockCode;
    private final String currentPrice;
    private final String previousClose;
    private final String change;
    private final String changeSign;
    private final String changeRate;
    private final String volume;
    private final String tradingAmount;
    private final String high;
    private final String low;
    private final String open;
    private final String marketType;
    private final String exchangeCode;
    private final String currency;
    private final BigDecimal exchangeRateValue;
    private final String currentPriceKrw;
    private final String cachedAt;
    private final String nextRefreshAt;
    private final long remainingSeconds;

    public static StockPriceResponse from(StockPrice price) {
        return from(price, "KRW", BigDecimal.ONE, Instant.now());
    }

    public static StockPriceResponse from(StockPrice price, String currency, BigDecimal exchangeRate) {
        return from(price, currency, exchangeRate, Instant.now());
    }

    public static StockPriceResponse from(StockPrice price, String currency, BigDecimal exchangeRate, Instant cachedAt) {
        String priceKrw = calculatePriceKrw(price.currentPrice(), exchangeRate);

        Duration ttl = Duration.ofMinutes(StockPriceCacheConfig.STOCK_PRICE_CACHE_TTL_MINUTES);
        Instant nextRefresh = cachedAt.plus(ttl);
        long remaining = Math.max(0, Duration.between(Instant.now(), nextRefresh).getSeconds());

        return new StockPriceResponse(
            price.stockCode(),
            price.currentPrice(),
            price.previousClose(),
            price.change(),
            price.changeSign(),
            price.changeRate(),
            price.volume(),
            price.tradingAmount(),
            price.high(),
            price.low(),
            price.open(),
            price.marketType().name(),
            price.exchangeCode().name(),
            currency,
            exchangeRate,
            priceKrw,
            cachedAt.toString(),
            nextRefresh.toString(),
            remaining
        );
    }

    private static String calculatePriceKrw(String currentPrice, BigDecimal exchangeRate) {
        if (currentPrice == null || currentPrice.isEmpty()) {
            return "0";
        }
        try {
            BigDecimal price = new BigDecimal(currentPrice.replace(",", ""));
            return price.multiply(exchangeRate).setScale(0, RoundingMode.HALF_UP).toPlainString();
        } catch (NumberFormatException e) {
            return "0";
        }
    }
}
