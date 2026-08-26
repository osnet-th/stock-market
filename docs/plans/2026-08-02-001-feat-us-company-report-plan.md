---
title: 기업 분석 리포트 미국 주식 확장
type: feat
status: active
date: 2026-08-02
issue: https://github.com/osnet-th/stock-market/issues/105
origin: docs/brainstorms/2026-08-02-us-company-report-brainstorm.md
gate: docs/gates/2026-08-02-us-company-report-gates.md
---

# 기업 분석 리포트 미국 주식 확장 (#105)

## Overview

기업 분석 리포트를 미국(SEC EDGAR) 종목으로 확장한다. 진입점은 기존 단일 검색창 유지(사전 시장 선택 없음, 거래소 배지 표시), 국가는 종목코드 형태로 판별해 KR(DART)/US(SEC) Assembler로 라우팅한다. 미지원 섹션(주주 동향)은 스냅샷에 명시하고 화면에서 제거+상단 안내 배너로 처리한다. 실측(AAPL)에서 발견된 `SecFinancialAdapter` 태그 폴백 버그(매출 null)를 본 작업에 포함해 수정한다.

## 현재 구조 (근거)

- 조립: `CompanyReportSnapshotService.assemble(stockCode)` — DART 기반 5개 서비스 병렬 수집 후 `ReportSnapshot` 조립, 개별 실패는 null 허용 (`CompanyReportSnapshotService.java:82-193`)
- KR 추출: `SnapshotFinancialExtractor`(863줄) — IFRS account_id 1순위 + 한글 계정명 폴백. **수정하지 않음**
- SEC 기연동: `SecApiClient`(companyfacts), `SecCikCache`(24h), `SecFinancialAdapter` — 연간 3년/분기 8개만 보관 (`SecFinancialAdapter.java:185-186`), 폴백은 "첫 번째 비어있지 않은 태그" 채택 (`getTagValues` 459-467행) → AAPL `Revenues` 태그가 FY2018 1건뿐이라 매출 null **(버그)**
- KIS 해외 현재가: `KisStockPriceAdapter` — `marketType.isDomestic()` 분기로 동작. 주가 히스토리는 국내 전용(`StockPriceService.java:103` KOSPI/KRX 하드코딩, 해외 default 빈 리스트)
- 종목코드 제약: `CreateCompanyReportRequest.java:14`·`CompanyAnalysisReport.java:111`(도메인 불변식)·`FinancialTimelineService.java:63`·`DisclosureQueryService.java:65` `\d{6}` 검증, `CompanyAnalysisReportEntity.java:35` `stock_code` length 6
- 원화 가정: `ReportSnapshot`에 통화 필드 없음(원 단위 주석), `GradeSuggestionCalculator.java:396` "억원", 프론트 억/조 포맷터(`company-report.js:1270` 등)
- 프론트 진입: 단일 검색창 + `company-report.js:189` KRX 필터
- 파생 계산: `CompanyReportReadService`가 조회 시 `LiquidationValueCalculator`/`DcfCalculator` 실행 — 입력(`valuationInputs`)은 통화 무관 숫자

## 승인 게이트 대상 (본 plan 승인 시 일괄 승인 처리)

1. **`ReportSnapshot` 스키마 변경**: `country`/`currency`/`unsupportedSections` 추가, schemaVersion 1→2. 기존 저장 스냅샷은 역직렬화 시 KR/KRW/빈 목록 기본값 (마이그레이션 없음)
2. **Entity 수정 1건**: `CompanyAnalysisReportEntity.stock_code` length 6→20 (ddl-auto 확대 반영 + 백업 SQL)
3. **공개 API 동작 변경**: 종목코드 검증 완화 — company-report 생성/미리보기 요청 DTO·도메인 불변식·`FinancialTimelineService`·`DisclosureQueryService`·price-history 검증을 `6자리 숫자 or 영문 티커(1~10자, 영대문자·점)` 허용으로 확대
4. **신규 공개 API 1건**: `GET /api/stocks/{ticker}/sec/filings` — US 공시 패널용 SEC 제출 서식 목록 (submissions 기반)
5. **구조 변경**: 리포트 조립을 국가별 전략으로 분리 — `ReportSnapshotAssembler` 인터페이스 + KR(기존 로직 이동)/US(신규) 구현체
6. **`SecFinancialAdapter` 수정**: 태그 폴백을 "최근 연도 커버 우선"으로 변경(기존 SEC 재무제표 화면 값도 수선됨), 보관 이력 연간 3→10년, 태그·dei 확장 — 기존 `SecFinancialPort` 공개 시그니처는 유지
7. **신규 외부 호출 2건**: SEC submissions API(`data.sec.gov/submissions/CIK{cik}.json`), KIS 해외주식 기간별시세 API

## 설계

### 국가 판별

- `stockCode.matches("\\d{6}")` → KR, 그 외(`^[A-Z][A-Z0-9.]{0,9}$`) → US. 별도 사용자 입력 없음
- US는 조립 시 `SecCikCache`에서 CIK 미존재 시 기존 `SecApiException` 흐름으로 실패 처리

### 스냅샷 스키마 (v2)

- `ReportSnapshot`에 추가: `country`("KR"/"US"), `currency`("KRW"/"USD"), `unsupportedSections`(List\<String\>, US는 `["shareholders"]`)
- `ReportSnapshotJsonMapper` 역직렬화: 필드 부재(v1) 시 KR/KRW/빈 목록 기본값 — 기존 저장 리포트 하위 호환
- 금액 문자열 규약: 통화 최소 단위 plain 문자열 유지 (KR: 원, US: 달러) — 파생 계산기(청산가치·DCF)는 숫자만 다루므로 불변

### 조립 전략 분리 (companyreport/application)

- `ReportSnapshotAssembler` 인터페이스: `boolean supports(String stockCode)` + `ReportSnapshot assemble(String stockCode)`
- `KrReportSnapshotAssembler`: 현 `CompanyReportSnapshotService` 로직 이동 (동작 불변, country=KR·currency=KRW만 추가)
- `UsReportSnapshotAssembler`(신규): SEC 타임라인 + 부가 데이터 병렬 수집(기존 `supplySafely` 패턴 준용)
- `CompanyReportSnapshotService`: 전략 목록에서 supports 매칭해 위임하는 라우터로 축소

### US 데이터 조달 매핑

| 스냅샷 섹션 | 소스 | 비고 |
|---|---|---|
| performance/statements/ratios | companyfacts us-gaap 연간 10년 | `UsSnapshotFinancialExtractor`(신규)가 태그 직접 매핑 — 실측 검증 태그 사용 (brainstorm 참조) |
| valuationInputs | 동일 | 현금·단기유가증권·매출채권·재고·장기투자·유형·무형/영업권·차입금 3종 — 태그 누락 시 null 허용 |
| priceMetrics | EPS/BPS(companyfacts) + KIS 해외 현재가 + 발행주식수(dei) | 시총=현재가×주식수, PER/PBR 계산. `ValuationMetricService`(DART 전용)는 미사용 |
| 배당 | `CommonStockDividendsPerShareDeclared`, `PaymentsOfDividends` | DividendRow로 매핑 |
| companyProfile | submissions API | 회사명·SIC 업종·결산월·거래소·주소. CEO명·설립일 등 미제공 필드 null |
| riskSignals | 발행주식수(dei) 추이 | 최근 5년 증가율 기준 희석 시그널. 서식 타입 기반 증자 탐지는 사용 안 함 |
| shareholders | — | null + `unsupportedSections: ["shareholders"]` |

### SEC 인프라 보강 (stock/infrastructure/stock/sec)

- **폴백 버그 수정**: `getTagValues`/`getQuarterlyTagValues`를 "최신 연도(분기)를 커버하는 태그 우선, 동률 시 데이터 포인트 많은 쪽" 선택으로 변경 — `getInvestmentMetrics`·재무제표 화면 동일 혜택
- **이력 확장**: `ParsedCompanyFacts` 연간 3→10년 (분기 8개 유지). 캐시 구조 불변
- **택소노미 확장**: us-gaap 추가 태그(valuationInputs·배당·이익잉여금·자본금 등) + dei(`EntityCommonStockSharesOutstanding`) 파싱 추가
- **`SecSubmissionsClient`(신규)**: 프로필 + 최근 제출 서식 목록. `SecCikCache` 재사용, Caffeine 24h 캐시
- `SecFinancialPort`에 리포트용 조달 메서드 추가(10년 연간 팩트·프로필·서식 목록) — 기존 3개 메서드 시그니처 불변

### 해외 주가 히스토리 (stock/infrastructure/stock/kis)

- `KisStockPriceClient`에 해외 기간별시세(월봉) 조회 추가, `KisStockPriceAdapter.getPriceHistory`가 `marketType.isDomestic()` 분기로 해외 구현
- `StockPriceService.getPriceHistory`의 KOSPI/KRX 하드코딩 제거 — 종목 마스터 캐시로 MarketType/ExchangeCode 해석

### Presentation

- `CreateCompanyReportRequest`·도메인 불변식·타임라인/공시 서비스 검증: `^\d{6}$|^[A-Z][A-Z0-9.]{0,9}$` (소문자 입력은 대문자 정규화)
- `SecFinancialController`에 `GET /api/stocks/{ticker}/sec/filings` 추가 (form/접수일/제목, 최근 N건)
- company-report preview/detail 응답은 스냅샷 v2 필드 그대로 노출 (별도 DTO 변경 없음 — 스냅샷 JSON 직렬화 경유)

### Frontend (static/js, partials)

- `company-report.js:189` KRX 필터 제거, 검색 결과에 거래소 배지(`KRX`/`NASDAQ`/`NYSE` 등)·통화 표시
- 상단 안내 배너: `unsupportedSections` 비어있지 않으면 "이 종목은 주주 동향 미제공" 형태로 1곳 표시. shareholders 섹션은 country=US면 렌더하지 않음
- 필드 단위 미제공(CEO명 등)은 "-" 표시
- 공시 패널: US면 `GET /api/stocks/{ticker}/sec/filings` 호출 + 라벨 "SEC 제출 서식"
- 금액 포맷터: `currency` 분기 — KRW 기존 억/조·원 유지, USD는 $·B/M 단위. `GradeSuggestionCalculator.java:396`의 "억원" 표기도 통화 인지로 수정
- 월봉 차트: 기존 `getStockPriceHistory` 호출 그대로 (백엔드 해외 지원으로 동작)

## Implementation Steps

### Phase 1: SEC 인프라 보강 + 폴백 버그 수정
- [x] `SecFinancialAdapter` 폴백 로직 수정 (최근 연도 커버 우선) — AAPL 매출 null 버그 해소
- [x] `ParsedCompanyFacts` 연간 10년 확장 + us-gaap 태그·dei 파싱 확장
- [x] `SecSubmissionsClient` 신규 (프로필·서식 목록, 캐시)
- [x] `SecFinancialPort` 리포트용 메서드 추가

### Phase 2: 조립 전략 분리 + US Assembler
- [x] `ReportSnapshot` v2 (country/currency/unsupportedSections) + `ReportSnapshotJsonMapper` 하위 호환
- [x] `ReportSnapshotAssembler` 인터페이스 + `KrReportSnapshotAssembler`(기존 로직 이동) + 라우터화
- [x] `UsSnapshotFinancialExtractor` + `UsReportSnapshotAssembler` (병렬 수집·null 허용)
- [x] `GradeSuggestionCalculator` 통화 인지 표기

### Phase 3: 제약 해제 + 프론트
- [x] 종목코드 검증 완화 4곳 + `CompanyAnalysisReportEntity.stock_code` length 20 + 백업 SQL
- [x] `GET /api/stocks/{ticker}/sec/filings` 추가
- [x] 프론트: KRX 필터 제거·거래소 배지·안내 배너·섹션 제거·"-" 표시·공시 패널 분기·USD 포맷터

### Phase 4: 해외 주가 히스토리
- [x] KIS 해외 기간별시세 클라이언트 + `KisStockPriceAdapter.getPriceHistory` 해외 구현
- [x] `StockPriceService` KOSPI/KRX 하드코딩 제거

## Validation

- `./gradlew compileJava` — 백엔드 컴파일
- `node --check` — 수정 JS 문법
- SEC 실호출: AAPL(태그 세대교체)·MSFT(일반)·JPM(금융주 태그 상이) 스냅샷 조립 확인 — 매출 버그 수정 회귀 포함
- KR 회귀: 005930 preview가 기존과 동일 스냅샷 생성 확인 (KrAssembler 이동 후)
- KIS 해외 기간별시세 실호출 — 계정의 해외주식 시세 권한 미신청이면 태형님에게 신청 요청
- UI 확인은 기존 목 하네스 방식(dev 로그인 제약) — 배지·배너·섹션 제거·USD 포맷
- 실서버 확인 항목은 validation 문서에 기록

## Risks

- 금융주 등 태그 체계 상이 업종은 폴백 확대 후에도 일부 항목 null 가능 — null 허용 설계로 부분 스냅샷 유지, JPM 검증으로 확인
- KIS 해외 시세·기간별시세는 계정 권한(해외주식 시세 신청) 의존 — 미신청 시 현재가/차트 공란, 신청 필요
- `KrReportSnapshotAssembler`로 기존 로직 이동 시 회귀 위험 — 로직 수정 없이 이동만, 005930 회귀 확인으로 방어
- SEC rate limit(초당 10요청) — 단일 사용자·캐시 24h 구조라 실질 리스크 낮음
- 기존 저장 스냅샷(v1) 조회 하위 호환 — 역직렬화 기본값으로 처리, 조회 화면 회귀 확인

## Out of Scope

- 주주 동향 섹션 미국 지원(SC 13D/G·DEF 14A 파싱) — 후속 이슈
- 미국 외 해외 시장(일본·중국·홍콩 등)
- 상용 재무 데이터 API 도입
- 챗봇·포트폴리오 등 타 기능의 해외 지원 확대
- 기존 KR 리포트 로직·화면 동작 변경 (스냅샷 필드 추가·Assembler 이동 제외)
