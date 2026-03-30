package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis;

import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import com.thlee.stock.market.stockmarket.stock.domain.model.StockPrice;
import com.thlee.stock.market.stockmarket.stock.domain.service.StockPricePort;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisDomesticMultiPriceOutput;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * KIS API를 통한 주식 현재가 조회 어댑터.
 * StockPricePort 구현체.
 */
@Component
public class KisStockPriceAdapter implements StockPricePort {

    private final KisStockPriceClient priceClient;
    private final CacheManager stockPriceCacheManager;

    public KisStockPriceAdapter(KisStockPriceClient priceClient,
                                @Qualifier("stockPriceCacheManager") CacheManager stockPriceCacheManager) {
        this.priceClient = priceClient;
        this.stockPriceCacheManager = stockPriceCacheManager;
    }

    @Cacheable(cacheManager = "stockPriceCacheManager", cacheNames = "stockPrice", key = "#stockCode + '_' + #exchangeCode")
    @Override
    public StockPrice getPrice(String stockCode, MarketType marketType, ExchangeCode exchangeCode) {
        if (marketType.isDomestic()) {
            return KisStockPriceMapper.fromDomestic(priceClient.getDomesticPrice(stockCode), stockCode, marketType, exchangeCode);
        }
        return KisStockPriceMapper.fromOverseas(priceClient.getOverseasPrice(stockCode, exchangeCode), stockCode, marketType, exchangeCode);
    }

    @Override
    public Map<String, StockPrice> getDomesticPrices(List<String> stockCodes) {
        Map<String, StockPrice> result = new LinkedHashMap<>();
        Cache cache = stockPriceCacheManager.getCache("stockPrice");

        // 캐시 히트 분리
        List<String> cacheMissCodes = new java.util.ArrayList<>();
        for (String code : stockCodes) {
            String cacheKey = code + "_" + ExchangeCode.KRX;
            StockPrice cached = cache != null ? cache.get(cacheKey, StockPrice.class) : null;
            if (cached != null) {
                result.put(code, cached);
            } else {
                cacheMissCodes.add(code);
            }
        }

        // 캐시 미스 종목을 30개씩 분할하여 멀티종목 API 호출
        for (int i = 0; i < cacheMissCodes.size(); i += 30) {
            List<String> batch = cacheMissCodes.subList(i, Math.min(i + 30, cacheMissCodes.size()));
            try {
                List<KisDomesticMultiPriceOutput> outputs = priceClient.getDomesticMultiPrice(batch);
                for (KisDomesticMultiPriceOutput output : outputs) {
                    StockPrice price = KisStockPriceMapper.fromDomesticMulti(output);
                    result.put(output.getStockCode(), price);
                    if (cache != null) {
                        cache.put(output.getStockCode() + "_" + ExchangeCode.KRX, price);
                    }
                }
            } catch (Exception e) {
                // 벌크 실패 시 개별 조회 폴백
                for (String code : batch) {
                    try {
                        StockPrice price = getPrice(code, MarketType.KOSPI, ExchangeCode.KRX);
                        result.put(code, new StockPrice(price.stockCode(), price.currentPrice(),
                            price.previousClose(), price.change(), price.changeSign(), price.changeRate(),
                            price.volume(), price.tradingAmount(), price.high(), price.low(), price.open(),
                            price.marketType(), price.exchangeCode()));
                    } catch (Exception ex) {
                        // 개별 조회도 실패 시 무시
                    }
                }
            }
        }

        return result;
    }
}