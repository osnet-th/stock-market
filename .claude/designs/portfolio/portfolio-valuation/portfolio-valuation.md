# 주식 현재가 일괄 조회 API

## 작업 리스트

- [x] 일괄 조회 요청/응답 DTO 생성
- [x] Caffeine 캐시 설정 (CacheConfig)
- [x] StockPriceService에 Caffeine 캐시 적용 + 일괄 조회 메서드 추가
- [x] StockController에 일괄 조회 엔드포인트 추가

## 배경

포트폴리오 화면에서 보유 주식의 현재가를 조회하여 수익률을 계산해야 한다. 현재 `GET /api/stocks/{stockCode}/price`는 단일 종목 조회만 지원하므로, 종목코드 리스트를 받아 현재가를 일괄 반환하는 API가 필요하다.

계산(평가금액, 수익률 등)은 프론트에서 처리한다. 백엔드는 현재가 제공에만 집중한다.

## 핵심 결정

- **기존 단일 조회 API 유지** — 하위 호환성 보장
- **`POST /api/stocks/prices`** 신규 엔드포인트 — 종목코드 리스트로 일괄 조회
- **내부적으로 종목별 순차 호출** — KIS API가 일괄 조회를 지원하지 않으므로 개별 호출 후 취합
- **Caffeine 캐시 (TTL 30분)** — 종목코드 + 거래소코드 기준 캐싱, 30분간 동일 가격 유지
- **개별 실패 허용** — 특정 종목 조회 실패 시 해당 종목만 null, 나머지는 정상 반환

## API 스펙

### `POST /api/stocks/prices`

요청:
```json
{
  "stocks": [
    { "stockCode": "005930", "marketType": "KOSPI", "exchangeCode": "KRX" },
    { "stockCode": "AAPL", "marketType": "NASDAQ", "exchangeCode": "NAS" }
  ]
}
```

응답:
```json
{
  "prices": {
    "005930": {
      "stockCode": "005930",
      "currentPrice": "72000",
      "previousClose": "71500",
      "change": "500",
      "changeSign": "2",
      "changeRate": "0.70",
      "volume": "12345678",
      "tradingAmount": "890000000000",
      "high": "72500",
      "low": "71000",
      "open": "71800",
      "marketType": "KOSPI",
      "exchangeCode": "KRX"
    },
    "AAPL": {
      "stockCode": "AAPL",
      "currentPrice": "178.50",
      "previousClose": "177.80",
      "change": "0.70",
      "changeSign": "2",
      "changeRate": "0.39",
      "volume": "55000000",
      "tradingAmount": null,
      "high": null,
      "low": null,
      "open": null,
      "marketType": "NASDAQ",
      "exchangeCode": "NAS"
    }
  }
}
```

- 응답의 `prices`는 종목코드를 키로 하는 Map — 프론트에서 종목코드로 바로 접근 가능
- 조회 실패한 종목은 value가 `null`

## 구현

### 요청 DTO

위치: `stock/presentation/dto/BulkStockPriceRequest.java`

```java
public class BulkStockPriceRequest {
    private List<StockPriceItem> stocks;

    public static class StockPriceItem {
        private String stockCode;
        private MarketType marketType;
        private ExchangeCode exchangeCode;
    }
}
```

### 응답 DTO

위치: `stock/application/dto/BulkStockPriceResponse.java`

```java
public class BulkStockPriceResponse {
    private final Map<String, StockPriceResponse> prices;
}
```

기존 `StockPriceResponse`를 재사용한다.

### StockPricePort 변경

기존 단일 조회 메서드는 유지. 일괄 조회 메서드를 추가하지 않는다 — 내부적으로 StockPriceService에서 단일 조회를 반복 호출한다.

### StockPriceService 변경

Caffeine 캐시를 단일 조회 메서드에 적용하고, 일괄 조회 메서드를 추가한다.

[예시 코드](./examples/stock-price-service-example.md)

### StockController 변경

기존 단일 조회 엔드포인트 유지, 일괄 조회 엔드포인트 추가.

[예시 코드](./examples/stock-controller-example.md)

### Caffeine 캐시 설정

- 키: `stockCode + "_" + exchangeCode` (예: `005930_KRX`)
- TTL: 30분
- CacheManager 빈 등록 필요

[예시 코드](./examples/cache-config-example.md)

## 주의사항

- KIS API는 일괄 조회를 지원하지 않으므로 종목 수만큼 개별 호출 발생, 30분 캐시로 완화
- 요청 종목 수 제한은 현재 범위에서 제외 (프론트에서 보유 종목만 요청하므로 대량 요청 가능성 낮음)
