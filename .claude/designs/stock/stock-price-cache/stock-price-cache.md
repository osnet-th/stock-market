# 주가 캐싱을 인프라 레이어로 이동

## 작업 리스트
- [x] KisStockPriceAdapter에 `@Cacheable` 적용
- [x] StockPriceService에서 `@Cacheable` 제거

## 배경
`StockPriceService.getPrices()`에서 `this.getPrice()`를 호출하면 Spring AOP 프록시를 거치지 않아 `@Cacheable`이 동작하지 않음. 캐싱 책임을 인프라 레이어(`KisStockPriceAdapter`)로 이동하여 해결.

## 핵심 결정
- 캐싱 위치: `KisStockPriceAdapter.getPrice()` (인프라 레이어, API 호출 직전)
- 캐시 키: `stockCode + '_' + exchangeCode` (기존과 동일)
- TTL: 30분, 최대 500건 (기존 `StockPriceCacheConfig` 그대로 사용)
- `StockPriceService`는 순수 비즈니스 로직만 담당

## 구현

### KisStockPriceAdapter
위치: `stock/infrastructure/stock/kis/KisStockPriceAdapter.java`

- `getPrice()` 메서드에 `@Cacheable` 추가
- 캐시 결과는 `StockPrice` 도메인 객체

[예시 코드](./examples/infrastructure-adapter-example.md)

### StockPriceService
위치: `stock/application/StockPriceService.java`

- `@Cacheable` 제거
- `@RequiredArgsConstructor` 복원

[예시 코드](./examples/application-service-example.md)

## 주의사항
- `StockPriceCacheConfig`는 변경 없음 (그대로 사용)
- 캐시 대상이 `StockPriceResponse` → `StockPrice`로 변경됨 (도메인 객체 캐싱)