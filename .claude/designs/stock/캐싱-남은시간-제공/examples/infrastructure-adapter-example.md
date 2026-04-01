# KisStockPriceAdapter 수동 캐시 + 타임스탬프 관리

## StockPriceCacheConfig.java 변경

```java
@Configuration
public class StockPriceCacheConfig {
    public static final String STOCK_PRICE_CACHE = "stockPrice";
    public static final long STOCK_PRICE_CACHE_TTL_MINUTES = 30;

    @Bean
    public CacheManager stockPriceCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(STOCK_PRICE_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(STOCK_PRICE_CACHE_TTL_MINUTES, TimeUnit.MINUTES)
            .maximumSize(500)
            .removalListener((key, value, cause) -> {
                // removalListener는 CacheManager 레벨에서는 설정 불가
                // 대신 Adapter에서 타임스탬프 정리
            }));
        return cacheManager;
    }
}
```

## StockPricePort.java 추가 메서드

```java
public interface StockPricePort {

    // 기존 메서드 유지
    StockPrice getPrice(String stockCode, MarketType marketType, ExchangeCode exchangeCode);
    Map<String, StockPrice> getDomesticPrices(List<String> stockCodes);

    // 캐시 정보 포함 메서드 추가
    CachedStockPrice getPriceWithCacheInfo(String stockCode, MarketType marketType, ExchangeCode exchangeCode);
    Map<String, CachedStockPrice> getDomesticPricesWithCacheInfo(List<String> stockCodes);
}
```

## KisStockPriceAdapter.java 변경

```java
@Component
public class KisStockPriceAdapter implements StockPricePort {

    private final KisStockPriceClient priceClient;
    private final CacheManager stockPriceCacheManager;
    private final KisDomesticMarketHours marketHours;
    private final ConcurrentHashMap<String, Instant> cacheTimestamps = new ConcurrentHashMap<>();

    // ... 생성자 동일

    // 기존 메서드: @Cacheable 제거, 수동 캐시로 전환
    @Override
    public StockPrice getPrice(String stockCode, MarketType marketType, ExchangeCode exchangeCode) {
        return getPriceWithCacheInfo(stockCode, marketType, exchangeCode).stockPrice();
    }

    // 새 메서드: 캐시 정보 포함
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

        // 캐시 미스: API 호출
        StockPrice price;
        if (marketType.isDomestic()) {
            price = getDomesticPriceByTime(stockCode, marketType, exchangeCode);
        } else {
            price = KisStockPriceMapper.fromOverseas(
                priceClient.getOverseasPrice(stockCode, exchangeCode), stockCode, marketType, exchangeCode);
        }

        Instant now = Instant.now();
        if (cache != null) {
            cache.put(cacheKey, price);
        }
        cacheTimestamps.put(cacheKey, now);

        return CachedStockPrice.of(price, now);
    }

    // getDomesticPricesWithCacheInfo도 동일 패턴으로 구현
    @Override
    public Map<String, CachedStockPrice> getDomesticPricesWithCacheInfo(List<String> stockCodes) {
        Map<String, CachedStockPrice> result = new LinkedHashMap<>();
        Cache cache = stockPriceCacheManager.getCache(StockPriceCacheConfig.STOCK_PRICE_CACHE);

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

        // 캐시 미스 종목 처리 (기존 로직 유지, 타임스탬프 기록 추가)
        Instant now = Instant.now();
        if (!cacheMissCodes.isEmpty()) {
            // ... 기존 멀티종목/시간외 분기 로직 유지
            // cache.put() 호출 시 cacheTimestamps.put(cacheKey, now) 함께 수행
        }

        return result;
    }

    // 기존 getDomesticPrices()는 getDomesticPricesWithCacheInfo() 위임
    @Override
    public Map<String, StockPrice> getDomesticPrices(List<String> stockCodes) {
        return getDomesticPricesWithCacheInfo(stockCodes).entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stockPrice(),
                (a, b) -> a, LinkedHashMap::new));
    }
}
```
