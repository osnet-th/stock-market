# 기업 분석 리포트 미국 주식 확장 Review 기록 (셀프 리뷰)

gate: docs/gates/2026-08-02-us-company-report-gates.md
work: docs/works/2026-08-02-us-company-report-work.md

## Findings (심각도순)

### H1. [수정 반영] 연간 추출이 10-K 내 분기(Q4)·전기 비교치 행을 오채택

- 근거: `SecFinancialAdapter.extractAnnualValues` — 10-K 파일링에 포함된 Q4 행·전기 비교치 행도 companyfacts에서 `fy/fp`가 파일링 기준(FY)으로 라벨되는데, 기존 dedup이 "마지막 엔트리"를 취해 과거 연도에서 Q4 값이 채택됨
- 실측: AAPL 10년 매출 병합 시 2016~2020이 46.9/52.6/62.9/64.0/64.7B(각 연도 Q4 값)로 오염. 신규 10년 리포트 팩트가 직접 영향, 기존 3년 화면도 이론상 노출
- 조치: 기간 데이터는 연간 기간(300~400일)만 채택 + 같은 연도 중복은 종료일(end) 최신 엔트리 선택. `fillQ4Fallback`의 FY start 수집에도 동일 필터 적용(Q4 start 혼입 시 누적 Q3 매칭 실패 방지)
- 재검증: 파이썬 재현으로 AAPL 10년 매출·FY2016 순이익(45.7B)·자산총계(321.7B) 전부 실제 공시값과 일치 확인. compileJava PASS

### M1. [확인 완료 — 문제 없음] StockPort 구현체 2개로 인한 빈 충돌 가능성

- `StockPriceService`·`UsReportSnapshotAssembler`가 `StockPort`를 신규 주입 — 구현체가 `KisStockAdapter`/`DataGoKrStockAdapter` 2개이나 `KisStockAdapter`에 `@Primary` 지정 확인 (`KisStockAdapter.java:13`). 기존 `StockSearchService`와 동일 주입 방식

### L1. [수정 반영 — 태형님 지시 2026-08-02] 주가지표 툴팁 문구가 DART/원 전제

- `company-report.js`의 `crMetricHelp` 고정 문구("DART 최신 사업보고서 기준" 등)가 미국 종목에서도 그대로 표시
- 조치: `crMetricHelpUs` 오버라이드(marketCap/eps/bps — SEC·10-K 출처 문구) 추가, `_crMetricHelpText`가 활성 스냅샷 통화로 선택. jsc 구문 검증 PASS

### L2. [수용 — 후속 이슈] 예상 매출(수동 입력) 단위가 KRW 전제

- 위저드의 예상 매출 입력 단위(억/조)는 원화 전제 — 미국 종목에서 사용자가 입력하면 차트 환산($B 나누기)과 의미가 어긋날 수 있음. 통화별 단위 선택지 설계가 필요해 후속 이슈로 분리 (태형님 확정 2026-08-02)

### L3. [수정 반영 — 태형님 지시 2026-08-02] 월봉 차트 통화 라벨 레이스

- preview 로드 완료 전에 월봉 차트가 먼저 렌더되면 통화 라벨이 KRW로 표시될 수 있음(차트 1회 렌더 가드)
- 조치: preview 로드 성공 시 `_crRerenderPriceHistoryChart()`로 기존 월봉 차트를 파기하고 확정 통화 라벨로 재렌더. jsc 구문 검증 PASS

### 관찰 (조치 없음)

- 분기 추출(`extractQuarterlyValues`)에도 전기 비교치 오염 가능성이 이론상 존재(기존 코드, 리포트 미사용 — 분기 화면은 10-Q 기준이라 영향 제한적). 후속 관찰
- `DAILY_HISTORY_CACHE` 키에 거래소 미포함 — 미국 티커는 거래소 간 심볼이 유일해 실질 충돌 없음
- 라우터의 Assembler 매칭은 패턴이 상호 배타적(6자리 숫자 vs 영문 시작)이라 주입 순서 무관

## Open Questions / Assumptions

- KIS 계정의 해외주식 시세(현재가·기간별) 권한이 신청되어 있다고 가정 — validation에서 실호출로 확인, 미신청이면 태형님께 신청 요청
- `LongTermDebt`(유동 포함 총액) 태그는 이중계상 위험 때문에 DEBT_NONCURRENT 체인에서 제외 — 일부 기업은 차입금이 null로 남을 수 있음(부분 스냅샷 허용)
- 미국 스냅샷의 fsDiv는 "CFS"(연결) 고정 — us-gaap 공시가 연결 기준

## Change Summary

SEC 어댑터 보강(폴백 커버리지 선택·연간 기간 필터·10년 병합·submissions), 리포트 조립 KR/US 전략 분리(스냅샷 v2: country/currency/unsupportedSections), 종목코드 티커 허용(도메인·DTO·Entity 20자), SEC filings API 신규, 프론트 단일 진입점(거래소 배지·미지원 섹션 배너·USD 포맷·공시 패널 분기), KIS 해외 기간별시세 연동. 코드 21개 수정 + 16개 신규.

## 재검증

- `./gradlew compileJava` PASS (H1 반영 후)
- 파이썬 실데이터 재현: 10년 매출 병합·잔여 자산 분류·차입금/순현금·희석 판정·배당 커버리지 검증 통과
- code-convention: 신규 메서드 5줄 기준 초과분은 데이터 변환·페이징 루프(예외 정책의 mapper/외부 호출 흐름 유지 사유)로 제한, 중첩 2단계 이내 유지
