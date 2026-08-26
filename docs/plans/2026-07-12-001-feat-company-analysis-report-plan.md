---
title: "feat: 기업분석리포트 — 등록/조회 (자동 산출 + 수동 입력 하이브리드)"
type: feat
status: active
date: 2026-07-12
origin: docs/brainstorms/2026-07-12-company-analysis-report-brainstorm.md
issue: https://github.com/osnet-th/stock-market/issues/81
gate: docs/gates/2026-07-12-company-analysis-report-gates.md
---

# feat: 기업분석리포트 — 등록/조회

## Overview

종목별 기업분석리포트를 작성(등록)·조회하는 기능. 정량 데이터(회사 개요, 10년 실적 추이, 재무제표 요약, 재무지표, 주가지표, 기업가치, 주주 동향, 위험 시그널)는 백엔드가 **스냅샷 JSON**으로 자동 산출·저장하고, 정성 분석(연혁/사업내용, 매입처/판매처 평가, 메모, 투자판단 7항목 A~E)은 태형님이 직접 입력한다. 화면은 "기업 리포트" 신규 탭. 국내(KRX/DART) 종목만.

## 확정된 결정 (brainstorm 승인, Q1~Q7)

| 항목 | 결정 |
|------|------|
| 구조 | 하이브리드: 정량 자동 산출 + 정성 수동 입력 |
| 스냅샷 | 저장 시 산출값 JSON 저장 + 수동 새로고침, 기준일 표시 |
| 투자판단 | 7항목 A~E **전부 수동 입력** (자동 제안 없음) |
| 실적 추이 | 10년 (백엔드 이미 MAX_YEARS=10 지원 → **무변경**, 호출 파라미터만 10) |
| 가치평가 파라미터 | 시스템 기본값(할인율 10%, 낙관 성장 20%, 조정자산 비율표) + 리포트별 조정 |
| 화면 | "기업 리포트" 신규 최상위 탭 |
| 주주 동향 | 최대주주 지분 추이 10년(hyslrSttus) + 5%룰 2년(majorstock) + 배당성향·자기주식(기존), 체결일은 DART 뷰어 링크 |

## 핵심 설계

### 스냅샷 = 원시 데이터, 가치평가 = 파생 계산 (중요)

- 스냅샷 JSON에는 **원시 수치**(타임라인, BS 카테고리 합계, FCF, 순현금, 시가총액 등)를 저장한다.
- 청산가치·DCF **결과값은 저장하지 않고**, 상세 조회 응답 시 `저장된 파라미터 × 스냅샷 원시 수치`로 백엔드가 파생 계산한다.
- 효과: 파라미터 수정 시 외부 API 재호출 없이 즉시 반영, 스냅샷은 "새로고침" 시에만 갱신.
- 스냅샷 JSON에 `schemaVersion` 필드 포함(향후 구조 진화 대비).

### Entity 설계 (Approval Gate — 본 plan 승인으로 확정)

`company_analysis_report` 단일 테이블(신규). 투자판단 7항목은 고정 항목이므로 컬럼으로, 자식 테이블 없음.

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT IDENTITY | PK |
| user_id | BIGINT not null | 작성자 (ID 참조, FK 없음, 조회 시 userId 스코프 강제) |
| stock_code | VARCHAR(6) not null | 종목코드 (ID/Code 참조 원칙) |
| stock_name | VARCHAR(100) | 종목명 (표시용 비정규화) |
| company_overview_note | TEXT | 연혁·경영이념·사업내용 (수동) |
| supplier_customer_note | TEXT | 매입처/판매처 분산 평가 (수동) |
| performance_note | TEXT | 실적 추이·경쟁사 비교 메모 (수동) |
| financial_change_note | TEXT | 재무제표 급변 원인 메모 (수동) |
| shareholder_note | TEXT | 주주 동향 메모 (수동) |
| judgment_comment | TEXT | 투자판단 종합 코멘트 (수동) |
| grade_asset_undervalue | VARCHAR(1) | 자산 저평가 A~E (`@Enumerated(STRING)`, nullable=미평가) |
| grade_earnings_undervalue | VARCHAR(1) | 수익 저평가 A~E |
| grade_financial_health | VARCHAR(1) | 재무건전성 A~E |
| grade_profitability | VARCHAR(1) | 수익성 A~E |
| grade_growth | VARCHAR(1) | 성장성 A~E |
| grade_business_competence | VARCHAR(1) | 사업역량 A~E |
| grade_shareholder_policy | VARCHAR(1) | 주주중시 A~E |
| valuation_params | JSONB not null | `{discountRate, optimisticGrowthRate, optimisticGrowthYears, adjustmentRatios{cash,securities,receivables,inventory,investments,tangible,intangible,otherCurrent}}` |
| snapshot | JSONB | 자동 산출 원시 데이터 (아래 스냅샷 구조) |
| snapshot_at | TIMESTAMP | 스냅샷 생성 시각 (기준일 표시용) |
| created_at | TIMESTAMP not null, updatable=false | |
| updated_at | TIMESTAMP not null | |

- 인덱스: `(user_id, stock_code)`, `(user_id, updated_at desc)`. 동일 종목 복수 리포트 허용(UNIQUE 없음) — 시점별 기록 목적.
- Enum `ReportGrade { A, B, C, D, E }` (domain/model).
- 컨벤션: `@Getter`만, protected 기본 생성자, JSON은 String 필드 + `@JdbcTypeCode(SqlTypes.JSON)` + 전용 Converter(참고: `DerivedFormulaJsonConverter`).

### 스냅샷 JSON 구조 (application DTO로 정의, 직렬화는 Converter)

```
{ schemaVersion: 1,
  companyProfile:      DART company.json (대표자, 설립일, 업종, 주소, 홈페이지 등),
  performanceTimeline: 10년 연간+올해 최신분기 — 매출/영업이익/순이익/원가율/판관비율/영업이익률/순이익률/영업CF/FCF,
  financialSummary:    BS·IS·CF 주요 항목 5년 (유동/비유동 자산·부채, 자본, 이익잉여금, 매출~순이익, CF 3종),
  financialRatios:     건전성(자기자본비율·유동비율·부채비율)/수익성(영업이익률·ROE·ROA)/성장성(매출·영업이익 성장률) 연도별 + 기준치 판정(good/warn/risk),
  priceMetrics:        현재가, 시가총액, EPS/BPS/PER/PBR(기존), PSR, PCFR, PER×PBR, EV/EBITDA, ROIC(근사), 발생액/총자산,
  valuationInputs:     BS 카테고리별 장부가(현금/유가증권/매출채권/재고/투자/유형/무형/기타유동), 총부채, 최근 FCF, 순현금(현금성+단기금융상품−총차입금), 미분류 계정 목록,
  shareholders:        최대주주·특수관계인 연도별 지분 추이 10년, 최근 2년 5%룰 변동 목록(보고일·증감·사유·rcept_no), 배당성향, 자기주식,
  riskSignals:         유동부채>유동자산, 자기자본비율≤20%, 순자산 음수, 재고·매출채권 급증(매출 증가율 대비), 증자 이력(최근 5년 공시검색) — 각 boolean + 근거 수치 }
```

### 신규 패키지 `companyreport/` (레이어 규칙 준수)

```
companyreport/
├── domain/
│   ├── model/        CompanyAnalysisReport, ReportGrade, ValuationParams, (스냅샷은 도메인 통과 시 String)
│   ├── repository/   CompanyAnalysisReportRepository (포트)
│   └── exception/    CompanyReportNotFoundException 등
├── application/
│   ├── CompanyReportWriteService      # 생성/수정/삭제 (+생성·새로고침 시 SnapshotService 호출)
│   ├── CompanyReportReadService       # 목록/상세 (상세 시 가치평가 파생 계산 포함)
│   ├── CompanyReportSnapshotService   # 기존 서비스 조합으로 스냅샷 조립 (ChatContextBuilder 선례)
│   │     주입: FinancialTimelineService, StockFinancialService, ValuationMetricService,
│   │           StockPriceService, DisclosureQueryService, (신규) CompanyProfile/Shareholder 조회
│   ├── LiquidationValueCalculator     # 조정자산 비율표 × BS 카테고리 → 청산가치
│   ├── DcfCalculator                  # 보수: 순현금+FCF/r / 낙관: 5년 g성장 후 무성장 영구가치 할인
│   └── dto/                           # SnapshotDto(직렬화 대상), *Command/*Result
├── infrastructure/persistence/        # Entity, JpaRepository, RepositoryImpl, mapper/(Mapper, SnapshotJsonConverter)
└── presentation/                      # CompanyReportController, dto/, ExceptionHandler, SecurityContext 헬퍼
```

### stock 도메인 확장 (Approval Gate — public 메서드/포트 추가)

DART 신규 엔드포인트 3종을 기존 DART 인프라(DartApiClient, DartCorpCodeCache)에 추가:

- `company.json` 기업개황 → `CompanyProfile` 도메인 모델
- `hyslrSttus.json` 최대주주 현황 (연도별 반복 호출로 10년 추이) → `MajorShareholder`
- `majorstock.json` 대량보유 상황보고 → `BulkHoldingReport`

포트: 기존 `StockFinancialPort` 비대화를 피해 **신규 포트 `CompanyDisclosurePort`**(stock/domain/service)로 분리, `DartCompanyAdapter`(infrastructure/stock/dart)가 구현. application에 `CompanyInfoService` 추가(스냅샷 서비스가 주입). stock 컨트롤러 엔드포인트 추가는 하지 않음(리포트 내부 용도만, 필요 시 후속).

### REST API (신규 공개 API — Approval Gate)

| 메서드 | 경로 | 역할 |
|--------|------|------|
| GET | `/api/company-reports/preview?stockCode=` | 저장 전 자동 산출 미리보기(스냅샷 조립, 저장 안 함) |
| POST | `/api/company-reports` | 생성 (수동 필드+등급+파라미터, 서버가 스냅샷 조립 후 함께 저장) → 201 + id |
| GET | `/api/company-reports?stockCode=&page=&size=` | 내 리포트 목록 (userId 스코프) |
| GET | `/api/company-reports/{id}` | 상세 (스냅샷 + 파생 가치평가 + 수동 필드) |
| PUT | `/api/company-reports/{id}` | 수동 필드/등급/파라미터 수정 (스냅샷 불변) |
| POST | `/api/company-reports/{id}/refresh` | 스냅샷 재조립(DART/KIS 재호출) + snapshot_at 갱신 |
| DELETE | `/api/company-reports/{id}` | 삭제 |

인증: 기존 JWT, `SecurityContext.currentUserId()` 패턴. 모든 조회/변경 userId 스코프(IDOR 방지, newsjournal 선례).

### 계산 정의 (스냅샷/파생)

- **청산가치** = Σ(카테고리 장부가 × 조정비율) − 총부채. 카테고리 매핑은 IFRS `account_id` 우선 + 계정명 키워드 폴백. 미매핑 계정은 0% 처리 + `unclassified` 목록으로 응답(투명성).
- **DCF 보수** = 순현금 + FCF/r. **낙관** = 순현금 + Σₜ₌₁⁵ FCF·(1+g)ᵗ/(1+r)ᵗ + [FCF·(1+g)⁵/r]/(1+r)⁵ (이후 무성장 영구가치).
- **EV/EBITDA**: EV = 시총 + 총차입금 − 현금성. EBITDA = 영업이익 + 감가상각비·무형상각비(CF표 조정 항목, 미검출 시 null 표기).
- **ROIC(근사)** = 영업이익 × (1 − 실효세율) / (자본총계 + 총차입금 − 현금성). 실효세율 = 법인세비용/세전이익(이상치 시 25% 대체). "근사" 라벨 명시.
- **발생액/총자산** = (당기순이익 − 영업CF) / 총자산.
- 데이터 결손 시 해당 지표 null + 화면 "—" 표시(예외 아님).

### 프론트엔드

- `app.js`: `menus`/`validPages`/`partialNames`/`navigateTo`에 `company-report` 추가, `...CompanyReportComponent` spread.
- `_sidebar.html`: 리포트 아이콘 SVG 추가.
- `partials/company-report.html`: ① 목록 뷰(내 리포트 테이블 + 종목 검색·새 리포트) ② 작성/수정 뷰(종목 선택 → "데이터 불러오기"(preview) → 자동 산출 미리보기 + 정성 입력 폼 + 7항목 등급 셀렉트 + 파라미터 조정) ③ 상세 뷰(7개 섹션 + 위험 시그널 배지 + A등급 매수재료 하이라이트 + 기준일/새로고침).
- `js/components/company-report.js`: 상태 bag `companyReport.*`(loading/saving 분리, newsjournal CRUD 패턴), 차트는 스냅샷 데이터 기반 자체 렌더(Chart.js, canvas 접두어 `report-`). financial.js 타임라인은 API 호출형이라 재사용하지 않고, 실적 추이 차트(금액+이익률 2축)만 경량 구현.
- `api.js`: `previewCompanyReport`, `createCompanyReport`, `getCompanyReports`, `getCompanyReport`, `updateCompanyReport`, `refreshCompanyReport`, `deleteCompanyReport`.
- 종목 검색: 기존 `GET /api/stocks/search` + KRX 필터(stock-eval 패턴 복제).

## Implementation Phases

### Phase 1 — stock 도메인 DART 확장 (백엔드) — 완료 (2026-07-12)
- [x] `CompanyDisclosurePort` 포트 + 도메인 모델(CompanyProfile, MajorShareholder, BulkHoldingReport)
- [x] DartApiClient에 company/hyslrSttus/majorstock 호출 추가 + `DartCompanyAdapter`
- [x] `CompanyInfoService`(application): 기업개황 1회 + 최대주주 연도별(10y, 병렬) + 5%룰 목록 조회
- [x] 실 데이터 검증: DART 원 API는 brainstorm 단계 curl 실호출로 검증 완료. 어댑터 경유 검증은 Phase 5(validation)에서 수행

### Phase 2 — companyreport 스냅샷/계산 (백엔드) — 완료 (2026-07-12)
- [x] SnapshotDto(schemaVersion=1) + `CompanyReportSnapshotService` 조립(타임라인 10y·요약·지표 판정·주가지표·valuationInputs·주주·위험 시그널)
- [x] `LiquidationValueCalculator` + `DcfCalculator` (+ ValuationParams 기본값 상수)
- [x] `GET /preview` 엔드포인트 구현 (조립 결과 수동 검증은 Phase 5)

### Phase 3 — Entity/CRUD (백엔드) — 완료 (2026-07-12)
- [x] Entity + 도메인 모델 + 포트/RepositoryImpl/Mapper + ValuationParamsJsonConverter/ReportSnapshotJsonMapper
- [x] Write/Read 서비스(@Transactional, 상세 조회 시 가치평가 파생 계산) + refresh
- [x] Controller + Request/Response DTO(record) + ExceptionHandler + SecurityContext 헬퍼

### Phase 4 — 프론트엔드 — 완료 (2026-07-12)
- [x] 메뉴/파티션/컴포넌트 등록 5단계 + 아이콘 (app.js/index.html/_sidebar.html 추가만, 기존 라인 수정 없음)
- [x] 목록 뷰 + 종목 검색 (stock-eval 패턴, KRX 필터)
- [x] 작성/수정 뷰(preview 불러오기 → 정성 폼 6종 → 투자판단 7항목 → 파라미터 조정 → 저장)
- [x] 상세 뷰(투자판단/개요/실적 차트+표/재무제표/지표 판정/주가지표/기업가치/주주 동향/위험 시그널 + 새로고침)

### Phase 5 — 검증
- [ ] 백엔드: preview/CRUD/refresh 실호출 검증(삼성전자 + 데이터 빈약 종목), userId 스코프 확인
- [ ] 프론트: 작성→저장→목록→상세→수정→새로고침 전체 플로우, 콘솔 에러 0
- [ ] 기존 화면(종목평가/포트폴리오) 회귀 없음 확인(수정 파일: app.js, index.html, _sidebar.html, api.js)
- [ ] `docs/validations/*.md` 기록

## Testing Strategy

- 수동 검증 우선(기존 관례). 계산기(청산가치·DCF)는 테스트 가능한 순수 클래스로 작성(테스트 코드는 명시 요청 시).
- 검증 종목: 삼성전자(데이터 풍부) + 신규 상장/소형주(결손 처리) + 부채과다 종목(위험 시그널).

## Risks / Trade-offs

- **DART 신규 3종 API**: 캐시 없음 → preview/refresh 시에만 호출(스냅샷 방식이라 빈도 낮음). 최대주주 10년 = 연도별 10회 호출 → 병렬 + 실패 연도 skip.
- **청산가치 매핑 정확도**: 키워드 폴백의 오분류 가능 → 미분류 목록 노출 + 카테고리별 금액 표시로 태형님이 검산 가능하게.
- **ROIC/EV/EBITDA 근사치**: 감가상각 미검출 등 → null 허용 + "근사" 라벨.
- **스냅샷 스키마 진화**: schemaVersion으로 방어, 구버전 스냅샷은 렌더 가능한 필드만 표시.
- **공용 파일 수정 회귀**: app.js/index.html/api.js는 추가만(기존 라인 수정 없음) 원칙.

## Approval Gates (본 plan 승인으로 처리되는 항목)

1. **Entity 신설**: `company_analysis_report` (위 스키마)
2. **신규 공개 API**: `/api/company-reports*` 7개 엔드포인트
3. **stock 도메인 public 확장**: `CompanyDisclosurePort` 신규 포트 + `CompanyInfoService` + DartApiClient 메서드 추가
4. **신규 패키지**: `companyreport/` (레이어 구조는 기존 규칙 그대로, 의존성 방향 변경 없음)

## 범위 추가 (validation 중 태형님 승인, 2026-07-13)

### A. 목록 필터: 종목명 부분일치 전용 — 완료
- [x] 종목코드 정확일치 필터 제거, `stockName` 부분일치(IgnoreCase)로 교체 (포트/JPA/Impl/Service/Controller/api.js/마크업)

### B. 작성 화면 위저드 개편 + 임시저장
결정: 작성은 7단계 위저드(항목별 자동 데이터 + 해당 수동 입력), 임시저장은 `draft` 플래그, 재개 위치는 `draft_step`으로 마지막 단계 기억(1번안).

단계: ① 종목 선택 ② 회사 개요 ③ 실적 추이 ④ 재무제표·지표 ⑤ 기업가치(파라미터) ⑥ 주주·위험 ⑦ 투자판단(작성 완료).

- Entity 컬럼 추가(Approval Gate — 본 범위 승인으로 처리): `draft boolean not null default false`, `draft_step smallint null`
- 흐름: 첫 임시저장 시 생성(스냅샷 1회 조립) → 이후 임시저장/완료는 빠른 갱신. 목록에서 draft는 "작성중" 배지 + 클릭 시 저장된 단계부터 위저드 재개(상세 조회 미제공). 완성 리포트 수정도 동일 위저드(2단계부터, 임시저장 대신 저장 버튼).

- [x] 백엔드: Entity/도메인/커맨드/요청/응답에 draft·draftStep 반영 (draftStep 2~7 검증) — 완료 (2026-07-14)
- [x] 프론트: 위저드(스텝 인디케이터+이전/다음), 임시저장/작성완료/저장 버튼 분기, draft 재개, 목록 배지·클릭 분기 — 완료 (2026-07-14)
- [x] 재검증: draft 생성(3단계)→목록 배지→갱신(5단계)→완료(draft=false)→잘못된 단계 400, API 레벨 통과 (2026-07-14). 위저드 화면 플로우는 태형님 테스트

### C. 정성 입력 구조화 (2026-07-14 승인)
자유 텍스트 6컬럼 → 구조화 JSON 컬럼 1개(`manual` jsonb, schemaVersion). 항목별 특화 입력:
연혁(연도+내용 행), 경영이념·사업내용(텍스트), 판매처/매입처(업체+비중+비고 행, 분산 평가 선택),
경쟁사 비교(경쟁사+사업부문+내용 행), 급변 항목(연도+항목+원인 행), 주주 이벤트(시기+내용 행)+메모, 투자판단 코멘트.
기존 테스트 리포트 메모 유실 승인됨. 상세 화면도 구조화 렌더(연혁 타임라인/비중 표 등)로 변경.

- [x] 백엔드: ReportManual 도메인 record(길이/행수 검증) + 컨버터, Entity manual jsonb 교체(노트 6컬럼 제거), 커맨드/요청/응답 반영 — 완료 (2026-07-14)
- [x] 프론트: 위저드 단계별 행 추가/삭제 편집기(연혁/판매·매입처/경쟁사/급변/주주이벤트), 상세 구조화 렌더(표) — 완료 (2026-07-14)
- [x] 재검증: 구조화 manual 왕복(생성→상세), 행 101개 상한 400, API 레벨 통과 (2026-07-14). 화면 플로우는 태형님 테스트

### D. 예상 매출 + 주가지표 계산기 (2026-07-14 승인)
- 예상 매출: 연도 행 추가 → 분기 4칸(+선택 연간 예상 순이익), **단위 사용자 선택(억/조)**, 실적 차트에 예상 연도 이어붙임
- 주가지표 계산기: 재료 입력(주가/유통주식수/EPS/BPS/매출/영업CF, 자동값 기본 채움) → PER·PBR·PSR·PCFR·PER×PBR 즉시 계산, 계산식 상시 표시. 예상 매출 기반 연도별 예상 PSR(+순이익 입력 시 예상 PER)
- 저장: ReportManual 확장(revenueForecasts/amountUnit/metricInputs) — manual JSONB 내부라 Entity 컬럼 변경 없음
- 판정 기준값 수정 기능은 태형님 지시로 **보류** (후속 후보)

- [x] 백엔드: ReportManual 확장(revenueForecasts/amountUnit/metricInputs) + 검증(연도 4자리, 금액 숫자, 단위 억/조) — 완료 (2026-07-14)
- [x] 프론트: 3단계 예상 매출 편집기(분기 4칸+연합계+선택 순이익, 단위 선택), 5단계 계산기(재료 입력→식·즉시 계산, 예상 PSR/PER), 상세 표 + 실적 차트에 예상 연도 막대 — 완료 (2026-07-14)
- [x] 재검증: 저장 왕복(2개년 예상+재료), 문자 금액 400, 잘못된 단위 400 — API 통과 (2026-07-14)

### E. 주가지표 툴팁 계산 근거값 노출 (2026-07-15 승인, 옵션 B)

- 툴팁을 native `title` → 커스텀 hover/클릭 팝오버로 교체(계산식이 안 보이던 문제 해결)
- 백엔드 `PriceMetrics.breakdowns` 신설: 각 지표 분자/분모/중간값을 구조화(terms/result/extras)로 노출 (스냅샷 응답 구조 확장 = Approval Gate, 하위호환 유지)
- 프론트: 공식 아래 "= 대입값 식 = 결과 (중간값)" 동적 조립
- 근거 결손 지표는 공식만 표시. EPS/BPS 결과값은 기존 valuation 산출값(근거는 참고), EV/EBITDA·ROIC는 근사식 중간값(상각비·실효세율)을 extras로 노출

- [x] 프론트: 툴팁 커스텀 팝오버 교체(배지 28곳, 헬퍼 4종) — 완료 (2026-07-15)
- [x] 백엔드: `PriceMetrics.breakdowns` + `SnapshotFinancialExtractor.buildBreakdowns` — 완료 (2026-07-15)
- [x] 프론트: `crBreakdownText` 동적 조립(view로 preview/detail 판별, 상세 내계산 6카드 제외) — 완료 (2026-07-15)
- [x] 재검증: Chrome 실동작(PSR/ROIC 근거·결측 공식만·클릭 고정) 통과 (2026-07-15, validation T1~T12)
- [x] 프론트: 재무제표 요약 표를 BS/IS/CF 3구역으로 분리(key 그룹핑, 백엔드 무변경) — 완료·검증 (2026-07-15, validation T13)
- [x] 백엔드: EPS/PER 폴백(valuation 계정명 정확일치 결측 시 timeline 당기순이익 ÷ 유통주식수 보정, 기존 valuation·종목평가 무변경) — 완료·검증 (2026-07-15, validation T14)

## Out of Scope

- 해외(SEC) 종목, 경쟁사 실적 자동 수집/비교, 5%룰 원문 파싱(체결일), PDF 내보내기
- 예상 수익률 컨센서스 연동(수동 입력)
- stock 도메인 컨트롤러에 기업개황/최대주주 공개 엔드포인트 추가(후속 후보)
- 기존 종목평가/포트폴리오 화면 변경
