package com.thlee.stock.market.stockmarket.stock.infrastructure.exchangerate.koreaexim;

import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorLatest;
import com.thlee.stock.market.stockmarket.economics.domain.repository.EcosIndicatorLatestRepository;
import com.thlee.stock.market.stockmarket.stock.domain.service.ExchangeRatePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 한국수출입은행 API를 통한 환율 조회 어댑터.
 * 전체 통화를 한 번에 조회하여 캐싱하고, 개별 통화 요청 시 캐시에서 반환한다.
 * 수출입은행 API 실패 시 ECOS 경제지표의 원/달러 환율을 fallback으로 사용한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KoreaEximExchangeRateAdapter implements ExchangeRatePort {

    private final KoreaEximExchangeRateClient client;
    private final EcosIndicatorLatestRepository ecosIndicatorLatestRepository;
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

    /**
     * ECOS 경제지표에서 통화코드 → 지표명 매핑.
     * ECOS 100대 통계의 "환율" 카테고리에서 조회한다.
     */
    private static final Map<String, String> ECOS_CURRENCY_MAP = Map.of(
        "USD", "원/달러"
    );

    @Cacheable(cacheManager = "exchangeRateCacheManager", cacheNames = "exchangeRate", key = "#currency")
    @Override
    public BigDecimal getRate(String currency) {
        if ("KRW".equals(currency)) {
            return BigDecimal.ONE;
        }

        loadRatesIfEmpty();

        BigDecimal rate = rateCache.get(currency);
        if (rate != null) {
            return rate;
        }

        // 수출입은행 캐시에 없으면 ECOS fallback 시도
        BigDecimal ecosRate = getEcosFallbackRate(currency);
        if (ecosRate != null) {
            log.info("수출입은행 환율 없음 ({}), ECOS fallback 사용: {}", currency, ecosRate);
            return ecosRate;
        }

        log.warn("환율 조회 실패 ({}): 수출입은행 + ECOS 모두 없음, DEFAULT_RATE(1) 반환", currency);
        return DEFAULT_RATE;
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
            log.warn("수출입은행 환율 API 호출 실패: {}", e.getMessage());
        }
    }

    /**
     * ECOS 경제지표 DB에서 환율 fallback 조회.
     * className="환율", keystatName="원/달러" 등에서 dataValue를 BigDecimal로 변환한다.
     */
    private BigDecimal getEcosFallbackRate(String currency) {
        String keystatName = ECOS_CURRENCY_MAP.get(currency);
        if (keystatName == null) {
            return null;
        }

        try {
            Optional<EcosIndicatorLatest> indicator =
                ecosIndicatorLatestRepository.findByClassNameAndKeystatName("환율", keystatName);

            return indicator
                .map(EcosIndicatorLatest::getDataValue)
                .filter(value -> value != null && !value.isEmpty())
                .map(value -> new BigDecimal(value.replace(",", "")))
                .orElse(null);
        } catch (Exception e) {
            log.warn("ECOS 환율 fallback 조회 실패 ({}): {}", currency, e.getMessage());
            return null;
        }
    }
}