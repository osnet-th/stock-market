# 기업분석리포트 월봉 주가 차트 Work 기록

gate: docs/gates/2026-07-16-company-report-monthly-price-chart-gates.md

## 변경 파일

### 백엔드 (stock 모듈 — companyreport 백엔드 무변경)
- `stock/domain/model/ChartPeriod.java` [신규] — D("D",140) / W("W",690) / M("M",3000). KIS FID_PERIOD_DIV_CODE + 주기별 페이징 창(캘린더일). `fromCode` 파싱.
- `stock/infrastructure/stock/kis/KisStockPriceClient.java` — `getDomesticDailyChart` → `getDomesticPeriodChart(+ChartPeriod)`. `FID_PERIOD_DIV_CODE="D"` 하드코딩 제거.
- `stock/domain/service/StockPricePort.java` — `getDailyHistory` → `getPriceHistory(+ChartPeriod)` 일반화 (기존 메서드 호출처 0곳 확인 후 대체).
- `stock/infrastructure/stock/kis/KisStockPriceAdapter.java` — `getPriceHistory`: 주기별 창 크기(`period.getChunkCalendarDays()`), 캐시 키 `stockCode:period:from:to`, 슬라이딩 페이징/dedup/graceful degrade 유지. 가드 상수 `MAX_CHUNK_ITERATIONS=10`(월봉 기준 ≈82년).
- `stock/application/StockPriceService.java` — `getPriceHistory(stockCode, period, from, to)`. from 기본 1960-01-01(상장일 조회 없이 전구간 — KIS가 실데이터 구간만 반환), to 기본 오늘. 국내(KOSPI/KRX) 고정.
- `stock/application/dto/PriceHistoryResponse.java` [신규] — `{stockCode, period, points[{date, open, high, low, close, volume}]}`.
- `stock/presentation/StockController.java` — `GET /api/stocks/{stockCode}/price-history?period=M&from=&to=`. 종목코드 6자리·period·날짜(yyyy-MM-dd) 검증(400).

### 프론트엔드
- `static/js/api.js` — `getStockPriceHistory(stockCode, period='M')` (timeoutMs 30s).
- `static/js/components/company-report.js` —
  - 상태 `companyReport.priceHistory { stockCode, points, loading, error, _gen }`.
  - `_crLoadPriceHistory(stockCode)`: 같은 종목 1회 로드 후 재사용, 세대 카운터 레이스 가드, 완료 시 현재 뷰에 렌더.
  - `_crRenderPriceHistoryChart(canvasId)`: 월봉 종가 라인(Chart.js), `Chart.getChart` 중복 렌더 가드, `_charts` push(기존 파기 흐름 편입).
  - 트리거: `_crRenderStepChart`(step 5 진입), `companyReportOpenDetail`, `companyReportRefresh`, `companyReportOnEnter`(detail 재진입).
- `static/partials/company-report.html` —
  - 위저드 5단계(기업가치) 상단: "주가 추이 (월봉)" 카드 + `report-wizard-price-history` canvas + 로딩/빈 상태.
  - 상세 "6. 주가지표" 섹션 내: 동일 구성 `report-detail-price-history` canvas.

## 설계 포인트
- 상장일 별도 조회 없음: from=1960-01-01 → KIS가 실데이터 존재 구간만 반환(첫 포인트 = 상장 시점). 실측 삼성전자 1985-01-31부터.
- 주가는 스냅샷 미저장(실시간 조회) + 서버 캐시 12h(dailyHistoryCacheManager).
- 페이징: 월봉 창 3000일 ≈ 98개월/호출 → 41년 = 5~6회 KIS 호출. 실측 총 0.5초.
- Port 시그니처 변경은 기존 호출처 0곳(미노출 상태였음) — plan 승인에 포함.
