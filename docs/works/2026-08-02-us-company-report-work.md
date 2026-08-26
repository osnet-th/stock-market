# 기업 분석 리포트 미국 주식 확장 Work 기록

gate: docs/gates/2026-08-02-us-company-report-gates.md
plan: docs/plans/2026-08-02-001-feat-us-company-report-plan.md
issue: https://github.com/osnet-th/stock-market/issues/105

## Phase 1: SEC 인프라 보강 + 폴백 버그 수정

- `SecFinancialAdapter`
  - **폴백 버그 수정**: `getTagValues`/`getQuarterlyTagValues`를 "첫 번째 비어있지 않은 태그" → "최신 연도(분기) 커버 태그 우선(동률 시 데이터 포인트 수)" 선택으로 변경. `getLatestValue`는 연도 우선 순회로 변경 — AAPL 매출 null 버그 해소 (기존 SEC 재무제표 화면도 수선됨)
  - **리포트 조달 추가**: `getAnnualReportFacts(ticker, years)` — 개념별 태그 폴백 체인(`CONCEPT_MAPPINGS`) + `mergeChain`(태그 세대교체 시 과거 이력 보존 병합) + 핵심 개념 기준 최근 10년 컬럼 산정
  - `getCompanyProfile`/`getRecentFilings` — submissions 기반 (Caffeine 24h 캐시, EDGAR 원문 `viewerUrl` 구성)
  - `ParsedCompanyFacts`에 shares 단위(`shareCountData`, us-gaap+dei) 추가
- `SecApiClient.fetchSubmissions` 추가 (companyfacts와 공통 fetch 헬퍼로 정리)
- DTO: `SecSubmissionsResponse` 신규, `SecCompanyFactsResponse`에 dei/shares 접근자 추가
- 도메인 모델 신규: `UsFinancialConcept`(개념 어휘), `UsCompanyFacts`, `UsCompanyProfile`, `UsFiling`
- `SecFinancialPort`에 리포트 조달 메서드 3종 추가 (기존 4개 메서드 시그니처 불변)

## Phase 2: 조립 전략 분리 + US Assembler

- `ReportSnapshot` v2: `country`/`currency`/`unsupportedSections` 추가, `CURRENT_SCHEMA_VERSION` 1→2, 통화 상수 추가
- `ReportSnapshotJsonMapper.fromJson`: v1 스냅샷 역직렬화 시 KR/KRW/빈 목록 기본값 (하위 호환)
- `ReportSnapshotAssembler` 인터페이스 신규
- `KrReportSnapshotAssembler`: 기존 `CompanyReportSnapshotService` 로직 이동 (로직 불변, country/currency 필드만 추가)
- `CompanyReportSnapshotService`: 전략 목록 위임 라우터로 축소 (public `assemble` 시그니처 불변 — Read/WriteService 수정 없음)
- `UsSnapshotFinancialExtractor` 신규: KR과 동일한 row key·항목명·판정 임계값으로 performance/statements/ratios/priceMetrics(+breakdowns)/valuationInputs/배당/riskSignals 생성. 유상증자 시그널은 발행주식수(최대 5개 연도) 증가율 >10% 희석 판정으로 대체
- `UsReportSnapshotAssembler` 신규: 팩트(10년) 필수 + 프로필/현재가(KIS 해외, 마스터→SEC 거래소명 폴백) 실패 시 null 허용. `unsupportedSections=["shareholders.major","shareholders.bulkHoldings"]` (배당은 제공)
- `GradeSuggestionCalculator`: 순현금 근거 표기 통화 인지 (KRW 억원 / USD $B)

## Phase 3: 제약 해제 + 프론트

- 검증 완화: `CreateCompanyReportRequest`(`\d{6}|[A-Za-z][A-Za-z0-9.\-]{0,9}`), `CompanyAnalysisReport`(도메인 불변식 + create 시 trim/대문자 정규화), `StockController` price-history
- `CompanyAnalysisReportEntity.stock_code` length 6→20 + 백업 SQL `db/migration/company_analysis_report_stock_code_length_2026_08_02.sql`
- 신규 API: `GET /api/stocks/{ticker}/sec/filings` (`SecFinancialService.getRecentFilings` + `SecFilingResponse`)
- 프론트 `company-report.js`: KRX 필터 → 지원 시장(KRX/NAS/NYS/AMS) 필터, 공시 패널 DART/SEC 분기(`_crLoadSecFilings` — 10-K/10-Q만), 통화 헬퍼(`crCurrency`/`crUnsupported`/`crUnsupportedNotice`/`_crUsdAmt`), `crAmt`/`_crBdVal`/실적 차트/월봉 차트 USD 분기, 거래소 배지·위험 시그널 라벨 헬퍼
- 프론트 `api.js`: `getSecFilings` 추가
- `company-report.html`: 검색 결과 거래소 배지, 위저드/상세 미지원 섹션 안내 배너, 최대주주·대량보유 서브섹션 `x-show` 숨김(배당 유지), 공시 패널 제목 동적화, CEO명 "-" 폴백, DART 문구 중립화

## Phase 4: 해외 주가 히스토리

- `KisOverseasDailyChartResponse` 신규 (HHDFS76240000 output2)
- `KisStockPriceClient.getOverseasPeriodChart` — GUBN(일0/주1/월2)·BYMD·MODP=1
- `KisStockPriceMapper.fromOverseasDailyChart`
- `KisStockPriceAdapter.getPriceHistory`: 국내/해외 분기 — 해외는 기준일(BYMD) 후방 이동 페이징(최대 10청크), 범위 필터, 부분 실패 degrade
- `StockPriceService.getPriceHistory`: KOSPI/KRX 하드코딩 제거 — 코드 형태로 시장 해석(티커는 종목 마스터, 미존재 시 NAS 가정)

## Plan 대비 편차

1. `FinancialTimelineService`/`DisclosureQueryService`의 6자리 검증은 **유지** — KR(DART) 전용 경로라 완화가 불필요하고, 티커 유입 시 조기 실패 가드로 유용 (plan 3번 항목 중 해당 부분 보류)
2. `SecSubmissionsClient` 별도 클래스 대신 `SecApiClient.fetchSubmissions` 메서드 + 어댑터 캐시로 구현 (기존 클라이언트 구조와 일관)
3. 프론트 검색은 필터 제거가 아니라 **지원 시장 화이트리스트**(KRX+미국 3거래소)로 구현 — 중국·일본 등 미지원 시장 종목이 리포트 진입점에 노출되지 않도록

## 검증 (work 단계)

- `./gradlew compileJava` PASS (Phase별 4회 + 최종)
- `jsc`(JavaScriptCore) 구문 검증: `company-report.js`, `api.js` PASS — 로컬에 node 미설치로 `node --check` 대체
- SEC/KIS 실호출·KR 회귀·UI 확인은 validation 단계 항목
