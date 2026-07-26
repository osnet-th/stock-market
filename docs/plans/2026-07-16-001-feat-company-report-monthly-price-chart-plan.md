---
title: "feat: 기업분석리포트 월봉 주가 차트 (상장일~현재)"
type: feat
status: active
date: 2026-07-16
issue: 95
origin: docs/brainstorms/2026-07-16-company-report-monthly-price-chart-brainstorm.md
---

# feat: 기업분석리포트 월봉 주가 차트 (상장일~현재)

gate: docs/gates/2026-07-16-company-report-monthly-price-chart-gates.md

## Overview

기업 분석 리포트의 위저드 5단계(기업가치, 주가지표 파티션)와 상세 뷰 "6. 주가지표" 섹션에
**상장일~현재 전구간 월봉 종가 라인 차트**를 추가한다.
KIS 기간별시세(FHKST03010100)의 주기 파라미터화 + 페이징으로 데이터를 확보하고,
신규 REST 엔드포인트로 노출한 뒤 기존 Chart.js 패턴으로 렌더한다.
(brainstorm: docs/brainstorms/2026-07-16-company-report-monthly-price-chart-brainstorm.md, issue #95)

## 확정된 결정 (brainstorm 승인)

| 항목 | 결정 |
|------|------|
| 기간/봉 | 상장일~현재 전구간 월봉 |
| 위치 | 위저드 5단계 주가지표 파티션 + 상세 "6. 주가지표" 섹션 |
| 차트 | 종가 라인 (Chart.js `_charts` 패턴) |
| 상장일 | 별도 조회 없이 from=1960-01-01 폴백 — KIS가 실데이터 구간만 반환 |
| 저장 | 스냅샷 미저장, 실시간 조회 + 서버 캐시(12h) |

## Proposed Solution — 변경 대상

### 백엔드 (stock 모듈 — companyreport 모듈 변경 없음)

```
stock/domain/model/
├── ChartPeriod.java                  # [신규] DAILY("D",140) / WEEKLY("W",690) / MONTHLY("M",3000)
│                                     #   KIS FID_PERIOD_DIV_CODE + 주기별 페이징 창(캘린더일)
stock/domain/service/
├── StockPricePort.java               # [수정] getDailyHistory → getPriceHistory(..., ChartPeriod, from, to)
stock/infrastructure/stock/kis/
├── KisStockPriceClient.java          # [수정] getDomesticDailyChart → getDomesticPeriodChart(+period)
│                                     #   FID_PERIOD_DIV_CODE 하드코딩("D") 제거 (line 114)
├── KisStockPriceAdapter.java         # [수정] getPriceHistory: 주기별 창 크기 + 캐시 키에 period 포함
stock/application/
├── StockPriceService.java            # [수정] getPriceHistory + 응답 DTO 매핑
├── dto/PriceHistoryResponse.java     # [신규] { stockCode, period, points[{date, close, open, high, low, volume}] }
stock/presentation/
├── StockController.java              # [수정] GET /api/stocks/{stockCode}/price-history 추가
```

- **신규 엔드포인트**: `GET /api/stocks/{stockCode}/price-history?period=M&from=&to=`
  - `period`: D|W|M (기본 M), `from` 기본 1960-01-01, `to` 기본 오늘. 종목코드 6자리 검증.
  - 국내 전용(KRX). 내부에서 MarketType.KOSPI/ExchangeCode.KRX 고정 전달(어댑터는 isDomestic만 검사).
- **시그니처 변경 근거**: `getDailyHistory`는 컨트롤러/타 모듈 호출처 0곳(어댑터·서비스 내부 위임뿐) —
  period 인자를 추가한 `getPriceHistory`로 일반화. 기존 동작(D)은 ChartPeriod.DAILY로 동일 재현.
- **페이징**: 기존 슬라이딩 윈도우 로직 유지, 창 크기만 주기별 상수화
  (M=3000일 ≈ 98개월/호출 → 40년 ≈ 5회, 가드 10회 내 ≈ 최대 82년).
- **캐시**: 동일 dailyHistoryCacheManager(12h), key = `stockCode:period:from:to`.

### 프론트엔드

```
static/js/api.js                      # [수정] getStockPriceHistory(stockCode, period='M')
static/js/components/company-report.js# [수정] priceHistory 상태 + 로드/렌더 헬퍼
static/partials/company-report.html   # [수정] 위저드 step5 + 상세 6번 섹션에 차트 카드/canvas
```

- 상태: `companyReport.priceHistory = { stockCode:'', points:[], loading, loaded, error, _gen }`
  — stockCode 기준 1회 로드 후 위저드/상세 양쪽 canvas에 재사용.
- 렌더: `_crRenderPriceHistoryChart(canvasId)` — 라벨 YYYY-MM, 종가 라인 1개, `_charts` push,
  `$nextTick` 후 호출, 뷰 전환 시 기존 `_crDestroyCharts()`가 함께 파기.
- canvas id: 위저드 `report-wizard-price-history`, 상세 `report-detail-price-history`.
- 트리거: 상세 열람 시(기존 상세 차트 렌더 지점) + 위저드 5단계 진입 시(`companyReportGoStep`).
  같은 종목 재진입은 로드 생략(캐시), 종목 변경 시 상태 리셋.
- 빈 상태: "주가 데이터를 불러올 수 없습니다" / 로딩 문구.

## Implementation Phases

### Phase 1 — 백엔드 (stock 모듈) — 완료
- [x] `ChartPeriod` enum 신설 (code, chunkCalendarDays: D=140/W=690/M=3000).
- [x] `KisStockPriceClient.getDomesticPeriodChart(stockCode, from, to, period)` — FID_PERIOD_DIV_CODE 파라미터화.
- [x] `StockPricePort.getPriceHistory(stockCode, marketType, exchangeCode, period, from, to)` (default 빈 리스트).
- [x] `KisStockPriceAdapter.getPriceHistory` — 주기별 창 크기, 캐시 키에 period 포함, 기존 dedup/degrade 유지.
- [x] `StockPriceService.getPriceHistory` + `PriceHistoryResponse` DTO. (compileJava 통과)
- [x] `StockController` GET `/api/stocks/{stockCode}/price-history` (period 기본 M, from 기본 1960-01-01, to 기본 오늘, 400 검증).

### Phase 2 — 프론트엔드 — 완료
- [x] `api.js` `getStockPriceHistory` (timeoutMs 30s).
- [x] `company-report.js` — `priceHistory` 상태, `_crLoadPriceHistory`(종목별 1회 로드·세대 가드), `_crRenderPriceHistoryChart`(Chart.getChart 중복 가드), `_crRenderPriceChartsForCurrentView`, 트리거(상세 open/refresh/onEnter + 위저드 step5 진입).
- [x] `company-report.html` — 위저드 step5 상단 + 상세 6번 주가지표 섹션 내 차트 카드(canvas/로딩/빈 상태). 태그 균형 검증.

### Phase 3 — 검증 — 완료
- [x] curl: `/api/stocks/005930/price-history` → **499포인트, 1985-01-31 ~ 2026-07-24 (41년), 0.5초**. 첫 포인트 = 상장 초기(1985).
- [x] 짧은 구간: `period=D&from=2026-06-01` → 38포인트 정상. 오류 검증: period=X → 400, 종목코드 5자리 → 400.
- [x] 라이브(dev 프로파일, 삼성전자): 위저드 5단계 + 상세 주가지표 섹션 모두 월봉 차트 렌더 확인(스크린샷). 신규 콘솔 에러 0(부동산 파티션 기존 에러 1건은 무관 확인).
- [x] 기존 실적 추이 차트 회귀 없음(상세 뷰 정상 렌더 확인).

## Testing Strategy

- 테스트 코드는 명시 요청 없음 → 수동 검증(curl + 라이브 브라우저).
- 회귀 관찰 대상: 기존 현재가/멀티시세(동일 어댑터 파일), 리포트 실적 추이 차트.

## Risks / Trade-offs

- **Port 시그니처 변경**: 호출처 0곳 확인됨 → 파급 없음. (gate: plan 승인에 포함)
- **KIS 연쇄 호출**: 월봉 전구간 ≈ 5회 순차 호출(레이트리밋 여유). 12h 캐시로 반복 조회 완충.
- **당월 미완성 월봉**: KIS가 완성 월까지만 줄 수 있음 — 그대로 표시(실측 기준).
- **위저드 미리보기 단계 이탈**: 종목 변경 시 priceHistory 리셋 누락하면 이전 종목 차트 잔존 → 리셋 지점 명시 구현.

## Approval Gates (이 작업)

- 신규 공개 API(`/api/stocks/{stockCode}/price-history`) + `StockPricePort` 시그니처 확장 → **본 플랜 승인에 포함**.
- Entity/DB/스냅샷 스키마 변경 없음. companyreport 백엔드 모듈 변경 없음.

## Out of Scope

- 캔들(OHLC)·주봉/년봉 토글 UI, 해외 종목, 종목평가 화면 변경, 스냅샷 저장 구조 변경.
