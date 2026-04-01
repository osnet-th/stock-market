package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis;

import com.thlee.stock.market.stockmarket.stock.domain.model.CachedStockPrice;
import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import com.thlee.stock.market.stockmarket.stock.domain.model.StockPrice;
import com.thlee.stock.market.stockmarket.stock.domain.service.StockPricePort;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.config.StockPriceCacheConfig;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisDomesticMultiPriceOutput;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisOvertimePriceOutput;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * KIS API를 통한 주식 현재가 조회 어댑터.
 * StockPricePort 구현체.
 */
@Component
public class KisStockPriceAdapter implements StockPricePort {

    private final KisStockPriceClient priceClient;
    private final CacheManager stockPriceCacheManager;
    private final KisDomesticMarketHours marketHours;
    private final ConcurrentHashMap<String, Instant> cacheTimestamps = new ConcurrentHashMap<>();

    public KisStockPriceAdapter(KisStockPriceClient priceClient,
                                @Qualifier("stockPriceCacheManager") CacheManager stockPriceCacheManager,
                                KisDomesticMarketHours marketHours) {
        this.priceClient = priceClient;
        this.stockPriceCacheManager = stockPriceCacheManager;
        this.marketHours = marketHours;
    }

    @Override
    public StockPrice getPrice(String stockCode, MarketType marketType, ExchangeCode exchangeCode) {
        return getPriceWithCacheInfo(stockCode, marketType, exchangeCode).stockPrice();
    }

    @Override
    public CachedStockPrice getPriceWithCacheInfo(String stockCode, MarketType marketType, ExchangeCode exchangeCode) {
        String cacheKey = stockCode + "_" + exchangeCode;
        Cache cache = stockPriceCacheManager.getCache(StockPriceCacheConfig.STOCK_PRICE_CACHE);

        if (cache != null) {
            StockPrice cached = cache.get(cacheKey, StockPrice.class);
            if (cached != null) {
                Instant cachedAt = cacheTimestamps.getOrDefault(cacheKey, Instant.now());
                return CachedStockPrice.of(cached, cachedAt);
            }
        }

        StockPrice price;
        if (marketType.isDomestic()) {
            price = getDomesticPriceByTime(stockCode, marketType, exchangeCode);
        } else {
            price = KisStockPriceMapper.fromOverseas(priceClient.getOverseasPrice(stockCode, exchangeCode), stockCode, marketType, exchangeCode);
        }

        Instant now = Instant.now();
        if (cache != null) {
            cache.put(cacheKey, price);
        }
        cacheTimestamps.put(cacheKey, now);

        return CachedStockPrice.of(price, now);
    }

    /**
     * 시간대에 따라 정규장/시간외 API를 분기하여 국내 주식 시세를 조회한다.
     * 시간외 현재가가 "0" 또는 빈 값이면 정규장 API로 폴백한다.
     */
    private StockPrice getDomesticPriceByTime(String stockCode, MarketType marketType, ExchangeCode exchangeCode) {
        if (marketHours.isOvertimeHours()) {
            KisOvertimePriceOutput overtimeOutput = priceClient.getDomesticOvertimePrice(stockCode);
            if (hasValidOvertimePrice(overtimeOutput)) {
                return KisStockPriceMapper.fromOvertime(overtimeOutput, stockCode, marketType, exchangeCode);
            }
        }
        return KisStockPriceMapper.fromDomestic(priceClient.getDomesticPrice(stockCode), stockCode, marketType, exchangeCode);
    }

    private boolean hasValidOvertimePrice(KisOvertimePriceOutput output) {
        return output.getCurrentPrice() != null
            && !output.getCurrentPrice().isEmpty()
            && !"0".equals(output.getCurrentPrice());
    }

    @Override
    public Map<String, StockPrice> getDomesticPrices(List<String> stockCodes) {
        Map<String, CachedStockPrice> cachedResult = getDomesticPricesWithCacheInfo(stockCodes);
        Map<String, StockPrice> result = new LinkedHashMap<>();
        cachedResult.forEach((key, value) -> result.put(key, value.stockPrice()));
        return result;
    }

    @Override
    public Map<String, CachedStockPrice> getDomesticPricesWithCacheInfo(List<String> stockCodes) {
        Map<String, CachedStockPrice> result = new LinkedHashMap<>();
        Cache cache = stockPriceCacheManager.getCache(StockPriceCacheConfig.STOCK_PRICE_CACHE);

        // 캐시 히트 분리
        List<String> cacheMissCodes = new ArrayList<>();
        for (String code : stockCodes) {
            String cacheKey = code + "_" + ExchangeCode.KRX;
            StockPrice cached = cache != null ? cache.get(cacheKey, StockPrice.class) : null;
            if (cached != null) {
                Instant cachedAt = cacheTimestamps.getOrDefault(cacheKey, Instant.now());
                result.put(code, CachedStockPrice.of(cached, cachedAt));
            } else {
                cacheMissCodes.add(code);
            }
        }

        Instant now = Instant.now();

        if (marketHours.isOvertimeHours()) {
            // 시간외 구간: 멀티종목 API 미지원 → 개별 조회
            for (String code : cacheMissCodes) {
                try {
                    CachedStockPrice cachedPrice = getPriceWithCacheInfo(code, MarketType.KOSPI, ExchangeCode.KRX);
                    result.put(code, cachedPrice);
                } catch (Exception e) {
                    // 개별 조회 실패 시 무시
                }
            }
        } else {
            // 정규장: 캐시 미스 종목을 30개씩 분할하여 멀티종목 API 호출
            for (int i = 0; i < cacheMissCodes.size(); i += 30) {
                List<String> batch = cacheMissCodes.subList(i, Math.min(i + 30, cacheMissCodes.size()));
                try {
                    List<KisDomesticMultiPriceOutput> outputs = priceClient.getDomesticMultiPrice(batch);
                    for (KisDomesticMultiPriceOutput output : outputs) {
                        StockPrice price = KisStockPriceMapper.fromDomesticMulti(output);
                        String cacheKey = output.getStockCode() + "_" + ExchangeCode.KRX;
                        if (cache != null) {
                            cache.put(cacheKey, price);
                        }
                        cacheTimestamps.put(cacheKey, now);
                        result.put(output.getStockCode(), CachedStockPrice.of(price, now));
                    }
                } catch (Exception e) {
                    // 벌크 실패 시 개별 조회 폴백
                    for (String code : batch) {
                        try {
                            CachedStockPrice cachedPrice = getPriceWithCacheInfo(code, MarketType.KOSPI, ExchangeCode.KRX);
                            result.put(code, cachedPrice);
                        } catch (Exception ex) {
                            // 개별 조회도 실패 시 무시
                        }
                    }
                }
            }
        }

        return result;
    }
}
