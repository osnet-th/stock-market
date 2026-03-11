# StockPriceService 예시

```java
@Service
@RequiredArgsConstructor
public class StockPriceService {

    private final StockPricePort stockPricePort;

    @Cacheable(cacheNames = "stockPrice", key = "#stockCode + '_' + #exchangeCode")
    public StockPriceResponse getPrice(String stockCode, MarketType marketType, ExchangeCode exchangeCode) {
        StockPrice price = stockPricePort.getPrice(stockCode, marketType, exchangeCode);
        return StockPriceResponse.from(price);
    }

    public BulkStockPriceResponse getPrices(List<BulkStockPriceRequest.StockPriceItem> stocks) {
        Map<String, StockPriceResponse> prices = new LinkedHashMap<>();

        for (BulkStockPriceRequest.StockPriceItem item : stocks) {
            try {
                StockPriceResponse response = getPrice(
                        item.getStockCode(),
                        item.getMarketType(),
                        item.getExchangeCode()
                );
                prices.put(item.getStockCode(), response);
            } catch (Exception e) {
                prices.put(item.getStockCode(), null);
            }
        }

        return new BulkStockPriceResponse(prices);
    }
}
```

- `getPrice()`에 `@Cacheable` 적용 — 일괄 조회 시 내부적으로 이 메서드를 호출하므로 캐시 자동 적용
- 개별 종목 조회 실패 시 해당 종목만 null, 나머지는 정상 처리