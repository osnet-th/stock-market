# KisStockPriceAdapter 변경 예시

## 변경 개요

- `KisDomesticMarketHours` 의존성 추가
- `getPrice()`: 시간외 구간이면 시간외 API 호출, 시간외 현재가가 없으면 정규장 폴백
- `getDomesticPrices()`: 시간외 구간이면 멀티종목 API 대신 개별 조회

```java
@Component
public class KisStockPriceAdapter implements StockPricePort {

    private final KisStockPriceClient priceClient;
    private final CacheManager stockPriceCacheManager;
    private final KisDomesticMarketHours marketHours;

    public KisStockPriceAdapter(KisStockPriceClient priceClient,
                                @Qualifier("stockPriceCacheManager") CacheManager stockPriceCacheManager,
                                KisDomesticMarketHours marketHours) {
        this.priceClient = priceClient;
        this.stockPriceCacheManager = stockPriceCacheManager;
        this.marketHours = marketHours;
    }

    @Cacheable(cacheManager = "stockPriceCacheManager", cacheNames = "stockPrice", key = "#stockCode + '_' + #exchangeCode")
    @Override
    public StockPrice getPrice(String stockCode, MarketType marketType, ExchangeCode exchangeCode) {
        if (marketType.isDomestic()) {
            return getDomesticPriceByTime(stockCode, marketType, exchangeCode);
        }
        return KisStockPriceMapper.fromOverseas(
            priceClient.getOverseasPrice(stockCode, exchangeCode), stockCode, marketType, exchangeCode);
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
            // 시간외 거래 없는 종목 → 정규장 종가로 폴백
        }
        return KisStockPriceMapper.fromDomestic(
            priceClient.getDomesticPrice(stockCode), stockCode, marketType, exchangeCode);
    }

    private boolean hasValidOvertimePrice(KisOvertimePriceOutput output) {
        return output.getCurrentPrice() != null
            && !output.getCurrentPrice().isEmpty()
            && !"0".equals(output.getCurrentPrice());
    }

    @Override
    public Map<String, StockPrice> getDomesticPrices(List<String> stockCodes) {
        Map<String, StockPrice> result = new LinkedHashMap<>();
        Cache cache = stockPriceCacheManager.getCache("stockPrice");

        // 캐시 히트 분리
        List<String> cacheMissCodes = new ArrayList<>();
        for (String code : stockCodes) {
            String cacheKey = code + "_" + ExchangeCode.KRX;
            StockPrice cached = cache != null ? cache.get(cacheKey, StockPrice.class) : null;
            if (cached != null) {
                result.put(code, cached);
            } else {
                cacheMissCodes.add(code);
            }
        }

        if (marketHours.isOvertimeHours()) {
            // 시간외 구간: 멀티종목 API 미지원 → 개별 조회
            for (String code : cacheMissCodes) {
                try {
                    StockPrice price = getPrice(code, MarketType.KOSPI, ExchangeCode.KRX);
                    result.put(code, price);
                } catch (Exception e) {
                    // 개별 조회 실패 시 무시
                }
            }
        } else {
            // 정규장: 기존 멀티종목 벌크 조회 로직 (변경 없음)
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
                            result.put(code, price);
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
```