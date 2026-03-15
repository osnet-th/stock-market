package com.thlee.stock.market.stockmarket.stock.infrastructure.exchangerate.koreaexim;

import com.thlee.stock.market.stockmarket.stock.domain.service.ExchangeRatePort;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 한국수출입은행 API를 통한 환율 조회 어댑터.
 * 전체 통화를 한 번에 조회하여 캐싱하고, 개별 통화 요청 시 캐시에서 반환한다.
 */
@Component
@RequiredArgsConstructor
public class KoreaEximExchangeRateAdapter implements ExchangeRatePort {

    private final KoreaEximExchangeRateClient client;
    private final Map<String, BigDecimal> rateCache = new ConcurrentHashMap<>();

    private static final BigDecimal DEFAULT_RATE = BigDecimal.ONE;

    /**
     * JPY(100), IDR(100), VND(100) 등 100단위 통화 처리.
     * 수출입은행은 JPY를 "JPY(100)" 코드로 반환하며, 환율도 100단위 기준이다.
     */
    private static final Map<String, String> CURRENCY_UNIT_MAP = Map.of(
        "JPY(100)", "JPY",
        "IDR(100)", "IDR",
        "VND(100)", "VND"
    );

    @Cacheable(cacheManager = "exchangeRateCacheManager", cacheNames = "exchangeRate", key = "#currency")
    @Override
    public BigDecimal getRate(String currency) {
        if ("KRW".equals(currency)) {
            return BigDecimal.ONE;
        }

        loadRatesIfEmpty();

        BigDecimal rate = rateCache.get(currency);
        return rate != null ? rate : DEFAULT_RATE;
    }

    private void loadRatesIfEmpty() {
        if (!rateCache.isEmpty()) {
            return;
        }

        try {
            List<KoreaEximExchangeRateClient.ExchangeRateItem> items = client.getExchangeRates();
            for (KoreaEximExchangeRateClient.ExchangeRateItem item : items) {
                String unit = item.getCurrencyUnit();
                String baseRateStr = item.getBaseRate();
                if (unit == null || baseRateStr == null) continue;

                BigDecimal baseRate = new BigDecimal(baseRateStr.replace(",", ""));

                // 100단위 통화 처리 (JPY(100) → JPY, 환율 / 100)
                String mappedCurrency = CURRENCY_UNIT_MAP.get(unit);
                if (mappedCurrency != null) {
                    rateCache.put(mappedCurrency, baseRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
                } else {
                    rateCache.put(unit, baseRate);
                }
            }
        } catch (Exception e) {
            // 조회 실패 시 빈 상태 유지, 다음 요청에서 재시도
        }
    }
}