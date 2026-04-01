# 시간외 현재가 조회 설계

## 작업 리스트

- [x] `KisOvertimePriceOutput` DTO 작성
- [x] `KisStockPriceMapper`에 `fromOvertime()` 매핑 메서드 추가
- [x] `KisDomesticMarketHours` 시간대 판단 클래스 작성
- [x] `KisStockPriceClient`에 `getDomesticOvertimePrice()` 메서드 추가
- [x] `KisStockPriceAdapter` 시간대 기반 분기 적용

## 배경

현재 국내 주식 시세 조회는 정규장 API(`FHKST01010100`)만 호출한다. 장 외 시간(15:40~18:00)에는 전일 종가가 그대로 반환되어, 시간외 단일가 거래 시세를 확인할 수 없다. 프론트 변경 없이 백엔드에서 시간대에 따라 API를 자동 분기한다.

## 핵심 결정

- **시간대 판단 위치**: infrastructure 레이어 (`KisDomesticMarketHours`). KIS API 호출 방식에 종속된 관심사이므로 domain에 두지 않는다.
- **시간외 API 범위**: 시간외 단일가(16:00~18:00) 구간만 시간외 API 호출. 장전 시간외(08:30~08:40)와 장후 시간외(15:40~16:00)는 거래 시간이 짧고 별도 API 스펙이 다르므로 1차 범위에서 제외한다.
- **응답 통일**: 시간외 응답 필드(`ovtm_untp_*`)를 기존 `StockPrice` 도메인 모델에 동일하게 매핑. 프론트/application 레이어 변경 없음.
- **멀티종목 조회**: 시간외 API는 멀티종목 벌크 조회를 지원하지 않으므로, 시간외 구간에서는 개별 조회로 폴백한다.

## 시간대 분기 기준

| 시간 (KST) | 호출 API | 비고 |
|---|---|---|
| 00:00 ~ 15:30 | 정규장 (`inquire-price`) | 장전/정규장 시간 |
| 15:31 ~ 15:59 | 정규장 (`inquire-price`) | 장후 시간외 (종가 거래, 정규장 API로 충분) |
| 16:00 ~ 18:00 | **시간외** (`inquire-overtime-price`) | 시간외 단일가 거래 |
| 18:01 ~ 23:59 | 정규장 (`inquire-price`) | 당일 종가 반환 |

## 구현

### KisOvertimePriceOutput

위치: `stock/infrastructure/stock/kis/dto/KisOvertimePriceOutput.java`

시간외 현재가 API(`FHPST02300000`) 응답 DTO. 필드명이 정규장과 다르므로(`ovtm_untp_*` 접두사) 별도 DTO가 필요하다.

[예시 코드](./examples/infrastructure-dto-example.md)

### KisDomesticMarketHours

위치: `stock/infrastructure/stock/kis/KisDomesticMarketHours.java`

`Clock`을 주입받아 현재 시각 기준으로 시간외 단일가 시간대 여부를 판단하는 유틸리티 클래스. `Clock` 주입으로 테스트 가능성을 확보한다.

[예시 코드](./examples/infrastructure-market-hours-example.md)

### KisStockPriceClient 변경

위치: `stock/infrastructure/stock/kis/KisStockPriceClient.java`

`getDomesticOvertimePrice(stockCode)` 메서드 추가. 기존 `getDomesticPrice`와 동일한 파라미터 구조, Path와 TR ID만 다름.

[예시 코드](./examples/infrastructure-client-example.md)

### KisStockPriceMapper 변경

위치: `stock/infrastructure/stock/kis/KisStockPriceMapper.java`

`fromOvertime()` static 메서드 추가. `KisOvertimePriceOutput` → `StockPrice` 매핑.

[예시 코드](./examples/infrastructure-mapper-example.md)

### KisStockPriceAdapter 변경

위치: `stock/infrastructure/stock/kis/KisStockPriceAdapter.java`

- `getPrice()`: `KisDomesticMarketHours`를 이용해 시간외 여부 판단 후, 정규장/시간외 API 분기
- `getDomesticPrices()`: 시간외 구간에서는 멀티종목 API 대신 개별 `getPrice()` 호출로 폴백

[예시 코드](./examples/infrastructure-adapter-example.md)

## 주의사항

- `Clock` 빈은 KIS 설정 클래스에서 `Clock.system(ZoneId.of("Asia/Seoul"))` 로 등록. 테스트 시 `Clock.fixed()`로 교체 가능.
- 시간외 API 응답에 `ovtm_untp_prpr`(시간외 현재가)이 `"0"` 또는 빈 값이면 해당 종목의 시간외 거래가 없는 것이므로, 정규장 API로 폴백한다.
- 캐시 키는 기존과 동일(`stockCode_KRX`). 시간대가 바뀌어도 캐시 TTL 내에서는 같은 값이 반환되므로, 기존 캐시 TTL이 적절한지 확인 필요.