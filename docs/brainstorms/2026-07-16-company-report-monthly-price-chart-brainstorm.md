# 기업분석리포트 월봉 주가 차트 - Brainstorm

gate: docs/gates/2026-07-16-company-report-monthly-price-chart-gates.md

**Date:** 2026-07-16
**Status:** Decided
**Issue:** #95 / Branch: `feat/issue-95-monthly-price-chart`

## What We're Building

기업 분석 리포트(작성 위저드 + 상세 뷰)에 **개별 회사 주가 차트(상장일~현재, 전구간 월봉, 종가 라인)**를 추가한다.

## 사전 실측 (KIS)

- `inquire-daily-itemchartprice` (FHKST03010100), `FID_PERIOD_DIV_CODE` = D/W/M/Y 지원.
- **호출당 최대 100건 하드 한계** (실측 D·W·M 정확히 100건). 응답은 종료일 기준 최신 100건.
- 페이징(윈도우 후방 이동)하면 과거 임의 구간 조회 가능 — 삼성전자 실측 1985년(~40년)까지 존재.
- 월봉 1호출 커버 ≈ 8.2년 → 40년치 ≈ 5회 호출.

## 확정된 결정 (사용자 확인 완료)

| 항목 | 결정 |
|------|------|
| 기간/봉 | 상장일~현재 **전구간 월봉** (삼성전자 기준 ~480포인트) |
| 위치 | **위저드 + 상세 뷰 둘 다** — 위저드 5단계(기업가치) 주가지표 파티션, 상세 "6. 주가지표" 섹션 |
| 차트 | **종가 라인** (Chart.js, 기존 `_charts` 패턴 재사용) |
| 상장일 | 별도 상장일 조회 없이 `from`을 과거로 넉넉히(예: 1960-01-01) → KIS가 실데이터 구간만 반환하는 특성 활용. 첫 데이터가 곧 상장 시점 |
| 저장 | 주가는 스냅샷에 저장하지 않고 **실시간 조회** (서버 캐시로 반복 조회 완충) |

## 현재 상태 (실측)

- KIS 일봉 체인(client→adapter→port→service→DailyPrice)은 구현돼 있으나 **컨트롤러 미연결**, `FID_PERIOD_DIV_CODE="D"` 하드코딩 (`KisStockPriceClient.java:114`).
- 어댑터 페이징: `DAILY_CHUNK_CALENDAR_DAYS=140`, `DAILY_MAX_CHUNK_ITERATIONS=10` — 일봉 전제라 월봉엔 창 크기 조정 필요.
- `@Cacheable` 키가 `stockCode:from:to` — period 미포함.
- 리포트 상세 응답에 stockCode 이미 존재. 위저드도 종목 선택 후 stockCode 보유.
- Chart.js 전역 로드됨. 기존 차트: `_crRenderPerfChart` + `_charts` push + `_crDestroyCharts`.

## Edge Cases

- 신규 상장(데이터 몇 개월뿐) → 있는 구간만 라인.
- KIS 실패/부분 실패 → 어댑터 graceful degrade(부분 반환) 유지, 프론트 빈 상태 문구.
- 리포트 재조회/뷰 전환 시 차트 destroy (기존 패턴).
- 페이징 반복 한도: 월봉 창 크기 조정으로 40년 ≈ 5회, 가드 10회 내.
- 당월 진행 중 월봉: KIS가 완성 월 기준으로 반환할 수 있음(실측 7/14 호출 시 최신 6/30) — 그대로 표시.

## 범위 밖 (하지 않음)

- 캔들(OHLC) 차트, 주봉/년봉 토글 UI.
- 종목평가 화면 변경.
- 스냅샷 저장 구조 변경(주가 미저장).
- 해외 종목 지원(국내 KRX만).
