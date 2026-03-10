# KIS 주식 현재가 조회 설계

## 작업 리스트

- [x] StockPricePort 도메인 포트 인터페이스 작성
- [x] StockPrice 도메인 모델 작성
- [x] StockPriceResponse 응답 DTO 작성
- [x] KIS API 응답 DTO 작성 (KisDomesticPriceResponse, KisOverseasPriceResponse)
- [x] KisStockPriceClient 작성 (국내/해외 현재가 API 호출)
- [x] KisStockPriceAdapter 작성 (StockPricePort 구현체)
- [x] ExchangeCode → KIS 해외 거래소 코드 매핑 메서드 추가 (name()으로 대체, 별도 필드 불필요)
- [x] StockPriceService 작성 (application 계층)
- [x] StockController에 현재가 조회 엔드포인트 추가

## 배경

종목 검색 기능은 구현 완료. 이제 검색된 종목의 현재가(시세)를 조회하는 기능이 필요하다. KIS Open API의 국내 주식 현재가 시세 API와 해외 주식 현재체결가 API를 사용한다.

## 핵심 결정

- **국내/해외 분기**: `MarketType.isDomestic()`으로 국내/해외 분기, 각각 다른 endpoint + tr_id 사용
- **ETF 조회**: 국내 ETF는 국내 주식과 동일 API (`fid_cond_mrkt_div_code=J`), 해외 ETF는 해외 주식과 동일 API — 별도 처리 불필요
- **도메인 모델 통합**: 국내/해외 응답 필드가 다르지만 `StockPrice` 도메인 모델로 통합 (공통 필드만 포함)
- **포트 분리**: 기존 `StockPort`(종목 검색)와 별도로 `StockPricePort`(현재가 조회) 포트 인터페이스 생성
- **KIS 인증 헤더**: 기존 `KisTokenManager.getAccessToken()` + `KisProperties`(appKey, appSecret) 활용

## KIS API 스펙

### 국내 주식 현재가 시세 API

| 항목 | 값 |
|------|---|
| Endpoint | `/uapi/domestic-stock/v1/quotations/inquire-price` |
| Method | GET |
| tr_id | `FHKST01010100` |

**요청 파라미터**:
- `FID_COND_MRKT_DIV_CODE`: `J` (주식/ETF/ETN)
- `FID_INPUT_ISCD`: 종목코드 6자리 (예: `005930`)

**응답 핵심 필드** (output):
- `stck_prpr`: 현재가
- `stck_oprc`: 시가
- `stck_hgpr`: 고가
- `stck_lwpr`: 저가
- `prdy_vrss`: 전일 대비
- `prdy_vrss_sign`: 전일 대비 부호 (1=상한, 2=상승, 3=보합, 4=하한, 5=하락)
- `prdy_ctrt`: 전일 대비율
- `acml_vol`: 누적 거래량
- `acml_tr_pbmn`: 누적 거래대금
- `w52_hgpr`: 52주 최고가
- `w52_lwpr`: 52주 최저가

### 해외 주식 현재체결가 API

| 항목 | 값 |
|------|---|
| Endpoint | `/uapi/overseas-price/v1/quotations/price` |
| Method | GET |
| tr_id | `HHDFS00000300` |

**요청 파라미터**:
- `AUTH`: 빈 문자열
- `EXCD`: 거래소코드 (NAS, NYS, AMS, HKS, TSE, SHS, SZS 등)
- `SYMB`: 종목코드 (예: `AAPL`)

**응답 핵심 필드** (output):
- `last`: 현재가
- `base`: 전일 종가
- `diff`: 전일 대비
- `sign`: 대비 부호
- `rate`: 등락률
- `tvol`: 거래량
- `tamt`: 거래대금
- `ordy`: 매수가능여부

### 공통 요청 헤더

```
content-type: application/json; charset=utf-8
authorization: Bearer {access_token}
appkey: {app_key}
appsecret: {app_secret}
tr_id: {거래ID}
```

## 구현

### StockPrice (도메인 모델)

위치: `stock/domain/model/StockPrice.java`

국내/해외 통합 도메인 모델 (record).

[예시 코드](./examples/domain-model-example.md)

### StockPricePort (도메인 포트)

위치: `stock/domain/service/StockPricePort.java`

종목코드 + MarketType + ExchangeCode로 현재가를 조회하는 포트 인터페이스.

[예시 코드](./examples/domain-model-example.md)

### KIS API 응답 DTO

위치: `stock/infrastructure/stock/kis/dto/`
- `KisDomesticPriceOutput.java` — 국내 현재가 응답 output 매핑
- `KisOverseasPriceOutput.java` — 해외 현재가 응답 output 매핑
- `KisPriceApiResponse.java` — 공통 응답 wrapper (rt_cd, msg_cd, msg1, output)

[예시 코드](./examples/infrastructure-dto-example.md)

### KisStockPriceClient

위치: `stock/infrastructure/stock/kis/KisStockPriceClient.java`

KIS API를 직접 호출하는 HTTP 클라이언트. `KisTokenManager`로 토큰을 가져오고, `RestClient`로 GET 요청.

[예시 코드](./examples/infrastructure-client-example.md)

### KisStockPriceAdapter

위치: `stock/infrastructure/stock/kis/KisStockPriceAdapter.java`

`StockPricePort` 구현체. `KisStockPriceClient` 호출 후 KIS DTO → `StockPrice` 도메인 모델 변환.

[예시 코드](./examples/infrastructure-adapter-example.md)

### ExchangeCode 매핑

`ExchangeCode`에 KIS 해외 API용 거래소코드 매핑 메서드 추가.

[예시 코드](./examples/domain-model-example.md)

### StockPriceService (application)

위치: `stock/application/StockPriceService.java`

[예시 코드](./examples/application-service-example.md)

### StockPriceResponse (응답 DTO)

위치: `stock/application/dto/StockPriceResponse.java`

[예시 코드](./examples/application-service-example.md)

### StockController 엔드포인트 추가

`GET /api/stocks/{stockCode}/price?marketType={}&exchangeCode={}`

[예시 코드](./examples/presentation-example.md)

## 주의사항

- 해외 주식 시세는 **무료시세(지연체결가)** 만 제공 — 실시간이 아님
- KIS API 호출 시 **쓰로틀링** 적용됨 (초당 요청 수 제한) — 클라이언트 측 대응은 현재 범위에 포함하지 않음
- 국내/해외 응답 필드명이 완전히 다름 — DTO를 분리하고 adapter에서 통합
- 해외 API 요청 시 `AUTH` 파라미터는 빈 문자열로 전달
- `prdy_vrss_sign`/`sign` 값 (대비 부호): 국내는 1~5, 해외는 동일 패턴 — 도메인 모델에서 String으로 보존