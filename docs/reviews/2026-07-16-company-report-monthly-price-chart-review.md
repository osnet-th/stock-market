# 기업분석리포트 월봉 주가 차트 Review

gate: docs/gates/2026-07-16-company-report-monthly-price-chart-gates.md

## Findings (심각도 순)

명시적 findings 없음 (버그·회귀·설계 위반·컨벤션 위반 없음). 아래는 남은 리스크/관찰.

- (low) **당월 미완성 월봉 포함**: 실측 마지막 포인트가 2026-07-24(진행 중인 7월) — KIS가 진행 월의 부분 봉을 반환함. 라인 차트 특성상 시각적 문제는 없으나 "완성 월"만 원하면 후속 조정 여지.
- (low) **KIS 미가동 시간/휴장일**: KIS 장애 시 어댑터 graceful degrade(부분/빈 리스트) → 프론트 "주가 데이터가 없습니다." 문구로 처리됨. 재시도 UI는 없음(리포트 재진입 시 재조회).
- (low) **Port 시그니처 변경**: `getDailyHistory` → `getPriceHistory`. 기존 호출처 0곳 grep 확인 — 파급 없음. 캐시 키에 period 포함으로 D/M 충돌 방지.
- (info) **위저드 즉시 재진입 렌더**: `Chart.getChart` 가드로 step 5 재진입 시 중복 생성 방지. 차트 파기는 기존 `_crDestroyCharts` 흐름(뷰 전환·preview 재로드·refresh)에 편입.
- (info) 상세 차트는 "6. 주가지표" 섹션(`priceMetrics` 존재 시) 내부에 위치 — priceMetrics 없는 리포트는 차트도 미표시(승인된 위치 결정에 따름).

## Open Questions / Assumptions

- 가정: KIS 기간별시세는 호출당 최대 100건, 종료일 기준 최신 반환(실측 확인).
- 가정: from=1960-01-01이면 KIS가 상장 이후 구간만 반환(삼성전자 1985-01-31 실측 확인).

## Change Summary

- 백엔드 7파일(신규 2: ChartPeriod, PriceHistoryResponse) — KIS 기간별시세 주기 파라미터화 + `/api/stocks/{stockCode}/price-history` 신설.
- 프론트 3파일 — 위저드 5단계·상세 주가지표 섹션에 월봉 종가 라인 차트.
- Entity/DB/스냅샷 스키마/companyreport 백엔드 무변경.
