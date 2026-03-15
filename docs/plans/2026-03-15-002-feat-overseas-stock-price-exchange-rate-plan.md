---
title: "feat: 해외 주식 현재가 반영 및 환율 변환"
type: feat
status: active
date: 2026-03-15
origin: docs/brainstorms/2026-03-15-overseas-stock-price-brainstorm.md
---

# feat: 해외 주식 현재가 반영 및 환율 변환

## Overview

포트폴리오에 등록된 해외 주식의 현재가를 KIS API로 조회하고, 환율을 적용하여 포트폴리오 총 평가금액에 원화로 합산한다. 개별 종목은 현지 통화로 표시하고(`195.00 USD`), 합산 시에만 원화 환산한다.

(see brainstorm: docs/brainstorms/2026-03-15-overseas-stock-price-brainstorm.md)

## Problem Statement / Motivation

해외 주식을 등록하면 현재가가 조회되지 않아 `investedAmount`만 표시되고, 평가금액/수익률이 계산되지 않는다. 또한 국내/해외 주식이 혼합된 포트폴리오에서 통화가 다른 금액이 단순 합산되어 잘못된 총액이 표시될 수 있다.

## Proposed Solution

### 서버에서 원화 환산가를 함께 내려주는 방식 채택

`StockPriceResponse`에 `currency`, `exchangeRate`, `currentPriceKrw` 필드를 추가하여, 프론트엔드는 표시만 하면 되도록 설계한다. 환율 조회/변환 로직을 서버에 집중시켜 프론트엔드 복잡도를 최소화한다.

## Technical Considerations

### Phase 1: 백엔드 - ExchangeCode 통화 매핑

`ExchangeCode` enum에 `currency` 필드 추가:

```
KRX -> KRW
NAS, NYS, AMS -> USD
SHS, SHI, SZS, SZI -> CNY
TSE -> JPY
HKS -> HKD
HNX, HSX -> VND
```

### Phase 2: 백엔드 - KIS 환율 조회

- `KisExchangeRateClient`: KIS API 환율 조회 클라이언트 신규 구현
- `ExchangeRatePort` / `ExchangeRateAdapter`: 헥사고날 포트/어댑터
- `ExchangeRateCacheConfig`: Caffeine 캐시 (TTL 1시간, 최대 20 엔트리)
- 환율 조회 실패 시: 캐시된 이전 값 사용, 캐시도 없으면 환율 1.0 적용 (investedAmount fallback과 동일 효과)

### Phase 3: 백엔드 - StockPriceResponse 확장

`StockPriceResponse`에 3개 필드 추가:

| 필드 | 타입 | 설명 | 예시 |
|------|------|------|------|
| currency | String | 통화코드 | "USD", "KRW" |
| exchangeRate | BigDecimal | 원화 환율 (1 외화 = N원) | 1380.50 (KRW는 1.0) |
| currentPriceKrw | String | 원화 환산 현재가 | "269,497" |

국내 주식: `currency="KRW"`, `exchangeRate=1.0`, `currentPriceKrw=currentPrice`
해외 주식: `currency="USD"`, `exchangeRate=1380.50`, `currentPriceKrw="269497"`

### Phase 4: 프론트엔드 - 현재가/평가금액 표시

**개별 종목 표시**:
- 국내: `72,000원` (기존과 동일)
- 해외: `195.00 USD` (현지 통화 + 통화코드)

**평가금액 계산** (`getEvalAmount`):
- `currentPriceKrw * quantity` 사용 (항상 원화 반환)
- 기존 `currentPrice * quantity`에서 변경

**수익률 계산** (`getProfitRate`):
- `(currentPrice - avgBuyPrice) / avgBuyPrice * 100` (현지 통화 기준, 환율 불필요)
- 기존 로직 유지

**수익금액 표시**:
- 개별 종목: 현지 통화로 표시 (`+350.00 USD`)
- 포트폴리오 합산: 원화 (`+483,000원`)

### investedAmount 통화 불일치 처리

해외 주식의 `avgBuyPrice`는 현지 통화로 저장되므로, `investedAmount`도 현지 통화이다.
- `getTotalInvested()`: 해외 주식의 `investedAmount`에 환율을 적용하여 원화로 합산
- 환율 정보는 `stockPrices` 응답의 `exchangeRate` 필드에서 가져옴

### 프론트엔드 변경 대상 함수

| 함수 | 변경 내용 |
|------|----------|
| `getEvalAmount(item)` | `currentPriceKrw` 사용으로 변경 |
| `getProfitAmount(item)` | 원화 기준: `getEvalAmount - (investedAmount * exchangeRate)` |
| `getTotalInvested()` | 해외 주식 `investedAmount`에 환율 적용 |
| `getStockPriceSummary(item)` | 해외 주식 통화 표시 (`195.00 USD`) |
| `getSubTotalInvested(type)` | 환율 적용 합산 |
| `getSubTotalEvalAmount(type)` | 이미 원화 반환이므로 변경 불필요 |

### 엣지 케이스

| 케이스 | 처리 |
|--------|------|
| 환율 조회 실패 | 캐시된 값 사용, 없으면 exchangeRate=1.0 |
| 해외 시장 폐장 | KIS API가 마지막 종가 반환 (기존 동작) |
| 해외 주식 현재가 조회 실패 | 기존 fallback (investedAmount 사용) |
| 소수점 처리 | 해외 주식 소수점 2자리, 국내 정수 |
| stockCode 키 충돌 | exchangeCode로 구분 (캐시 키: `stockCode_exchangeCode`) |

## Acceptance Criteria

### Phase 1: ExchangeCode 통화 매핑
- [x] `ExchangeCode` enum에 `currency` 필드 추가 (`ExchangeCode.java`)
- [x] 12개 거래소 → 6개 통화 매핑 완성

### Phase 2: 환율 조회
- [x] `KisExchangeRateClient` 구현 - KIS API 환율 조회
- [x] `ExchangeRatePort` 인터페이스 정의 (`domain/service/`)
- [x] `KisExchangeRateAdapter` 구현 (`infrastructure/`)
- [x] `StockPriceService`에서 `ExchangeRatePort` 주입하여 환율 조회
- [x] Caffeine 캐시 설정 (TTL 1시간)

### Phase 3: StockPriceResponse 확장
- [x] `StockPriceResponse`에 `currency`, `exchangeRateValue`, `currentPriceKrw` 추가
- [x] `StockPriceService.getPrice()`에서 환율 조회 후 응답에 포함
- [x] `ExchangeCode.getCurrency()`로 통화 매핑

### Phase 4: 프론트엔드
- [x] `getEvalAmount()` → `currentPriceKrw` 사용으로 변경 (`app.js`)
- [x] `getTotalInvested()` → 해외 주식 환율 적용 합산 (`app.js`)
- [x] `getProfitAmount()` → 원화 기준 수익금 계산 (`app.js`)
- [x] `getStockPriceSummary()` → 해외 주식 통화 표시 (`app.js`)
- [x] 개별 종목 원금 원화 환산 표시 (`index.html`)
- [x] 도넛 차트/막대 차트 정상 반영 (기존 로직이 원화 기반이므로 자동 반영)

## Dependencies & Risks

- **KIS 환율 API 스펙 확인 필요**: KIS API 문서에서 환율 조회 TR의 요청/응답 형태를 확인해야 함
- **KIS API 호출 제한**: 환율 조회 추가로 인한 API 호출량 증가 → 캐싱으로 완화
- **VND, CNY 등 마이너 통화**: KIS API에서 지원하지 않을 수 있음 → 미지원 통화는 환율 1.0 fallback

## Sources & References

- **Origin brainstorm:** [docs/brainstorms/2026-03-15-overseas-stock-price-brainstorm.md](docs/brainstorms/2026-03-15-overseas-stock-price-brainstorm.md) — KIS API 활용, 현지 통화 표시, 원화 환산 합산, 기존 API 확장
- KIS 해외 현재가: `src/.../infrastructure/stock/kis/KisStockPriceClient.java:49` (TR_ID: HHDFS00000300)
- KIS 어댑터: `src/.../infrastructure/stock/kis/KisStockPriceAdapter.java:23`
- StockPriceResponse: `src/.../application/dto/StockPriceResponse.java`
- ExchangeCode enum: `src/.../domain/model/ExchangeCode.java`
- 캐시 설정: `src/.../infrastructure/stock/kis/config/StockPriceCacheConfig.java`
- 프론트 현재가 로드: `src/main/resources/static/js/app.js:496` (loadStockPrices)
- 평가금액 계산: `src/main/resources/static/js/app.js:522` (getEvalAmount)
- 기존 설계: `.claude/designs/stock/stock-price-cache/stock-price-cache.md`
- 토큰 관리: `.claude/designs/stock/kis-token-management/kis-token-management.md`