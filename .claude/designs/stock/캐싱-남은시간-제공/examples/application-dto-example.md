# StockPriceResponse DTO 확장

## StockPriceResponse.java 변경

```java
@Getter
@RequiredArgsConstructor
public class StockPriceResponse {

    // ... 기존 필드 유지

    private final String currency;
    private final BigDecimal exchangeRateValue;
    private final String currentPriceKrw;

    // 캐시 메타데이터 추가
    private final String cachedAt;          // ISO 8601 (예: "2026-04-01T07:30:00Z")
    private final String nextRefreshAt;     // ISO 8601
    private final long remainingSeconds;    // 갱신까지 남은 초

    // 기존 팩토리 메서드 유지 (하위 호환)
    public static StockPriceResponse from(StockPrice price) {
        return from(price, "KRW", BigDecimal.ONE, Instant.now());
    }

    public static StockPriceResponse from(StockPrice price, String currency, BigDecimal exchangeRate) {
        return from(price, currency, exchangeRate, Instant.now());
    }

    // 캐시 정보 포함 팩토리 메서드
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

    // ... calculatePriceKrw 기존 메서드 유지
}
```

## StockPriceService.java 변경

```java
@Service
@RequiredArgsConstructor
public class StockPriceService {

    private final StockPricePort stockPricePort;
    private final ExchangeRatePort exchangeRatePort;

    public StockPriceResponse getPrice(String stockCode, MarketType marketType, ExchangeCode exchangeCode) {
        CachedStockPrice cached = stockPricePort.getPriceWithCacheInfo(stockCode, marketType, exchangeCode);
        String currency = exchangeCode.getCurrency();
        BigDecimal exchangeRate = exchangeRatePort.getRate(currency);
        return StockPriceResponse.from(cached.stockPrice(), currency, exchangeRate, cached.cachedAt());
    }

    public BulkStockPriceResponse getPrices(List<BulkStockPriceRequest.StockPriceItem> stocks) {
        Map<String, StockPriceResponse> prices = new LinkedHashMap<>();

        List<BulkStockPriceRequest.StockPriceItem> domesticStocks = new ArrayList<>();
        List<BulkStockPriceRequest.StockPriceItem> overseasStocks = new ArrayList<>();

        for (BulkStockPriceRequest.StockPriceItem item : stocks) {
            if (item.getMarketType().isDomestic()) {
                domesticStocks.add(item);
            } else {
                overseasStocks.add(item);
            }
        }

        // 국내 주식: 캐시 정보 포함 조회
        if (!domesticStocks.isEmpty()) {
            List<String> stockCodes = domesticStocks.stream()
                .map(BulkStockPriceRequest.StockPriceItem::getStockCode)
                .toList();
            Map<String, CachedStockPrice> domesticPrices = stockPricePort.getDomesticPricesWithCacheInfo(stockCodes);
            BigDecimal krwRate = exchangeRatePort.getRate("KRW");

            for (BulkStockPriceRequest.StockPriceItem item : domesticStocks) {
                CachedStockPrice cached = domesticPrices.get(item.getStockCode());
                if (cached != null) {
                    prices.put(item.getStockCode(),
                        StockPriceResponse.from(cached.stockPrice(), "KRW", krwRate, cached.cachedAt()));
                } else {
                    prices.put(item.getStockCode(), null);
                }
            }
        }

        // 해외 주식: 기존 개별 조회 (캐시 정보 포함)
        for (BulkStockPriceRequest.StockPriceItem item : overseasStocks) {
            try {
                StockPriceResponse response = getPrice(item.getStockCode(), item.getMarketType(), item.getExchangeCode());
                prices.put(item.getStockCode(), response);
            } catch (Exception e) {
                prices.put(item.getStockCode(), null);
            }
        }

        return new BulkStockPriceResponse(prices);
    }
}
```
