# Presentation 계층 예시

## StockController 엔드포인트 추가

기존 `StockController`에 현재가 조회 엔드포인트를 추가한다.

```java
// 기존 StockController에 추가할 필드 및 메서드

private final StockPriceService stockPriceService;

/**
 * 주식/ETF 현재가 조회.
 *
 * @param stockCode    종목코드 (국내: 005930, 해외: AAPL)
 * @param marketType   시장구분 (KOSPI, NASDAQ 등)
 * @param exchangeCode 거래소코드 (KRX, NAS 등)
 */
@GetMapping("/{stockCode}/price")
public ResponseEntity<StockPriceResponse> getPrice(
        @PathVariable String stockCode,
        @RequestParam MarketType marketType,
        @RequestParam ExchangeCode exchangeCode) {
    StockPriceResponse response = stockPriceService.getPrice(stockCode, marketType, exchangeCode);
    return ResponseEntity.ok(response);
}
```

### API 호출 예시

**국내 주식 (삼성전자)**:
```
GET /api/stocks/005930/price?marketType=KOSPI&exchangeCode=KRX
```

**해외 주식 (Apple)**:
```
GET /api/stocks/AAPL/price?marketType=NASDAQ&exchangeCode=NAS
```

**국내 ETF (KODEX 200)**:
```
GET /api/stocks/069500/price?marketType=KOSPI&exchangeCode=KRX
```

**해외 ETF (SPY)**:
```
GET /api/stocks/SPY/price?marketType=NYSE&exchangeCode=NYS
```

### 참고

- `MarketType`, `ExchangeCode`는 enum이므로 Spring이 자동으로 `@RequestParam`에서 문자열 → enum 변환
- 잘못된 enum 값 전달 시 Spring이 400 Bad Request 반환