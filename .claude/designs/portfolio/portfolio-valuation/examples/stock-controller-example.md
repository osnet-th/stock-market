# StockController 예시

```java
@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockSearchService stockSearchService;
    private final StockPriceService stockPriceService;

    // ... 기존 엔드포인트 유지 ...

    @PostMapping("/prices")
    public ResponseEntity<BulkStockPriceResponse> getPrices(
            @RequestBody BulkStockPriceRequest request) {
        BulkStockPriceResponse response = stockPriceService.getPrices(request.getStocks());
        return ResponseEntity.ok(response);
    }
}
```