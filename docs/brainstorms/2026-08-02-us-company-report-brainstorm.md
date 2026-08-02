# 기업 분석 리포트 미국 주식 확장 - Brainstorm

**Date:** 2026-08-02
**Status:** Decided (태형님 요청 + 방향 확정, 2026-08-02)
gate: docs/gates/2026-08-02-us-company-report-gates.md

## 배경

- 태형님 요청: "기업 분석 리포트가 현재 한국 주식만 지원하는데 해외 주식도 가능한지, 가능하다면 어떻게 확장하고 어디서 정보를 가져올지" → 조사 후 "미국 주식 기준으로만 판단" → "어댑터 매출 버그도 기능 확장하면서 같이 수정하는 쪽으로 진행" 확정
- 기존 구조 (이미 있는 것):
  - 기업 리포트: `CompanyReportSnapshotService`가 DART 기반 서비스 5종(재무 타임라인·배당/주식총수·밸류에이션·기업개황/주주·공시검색)을 병렬 수집해 `ReportSnapshot` 조립. LLM 미사용, 결정론적 계산
  - SEC EDGAR 연동: `stock/infrastructure/stock/sec/` — `SecApiClient`(companyfacts), `SecCikCache`(24h 캐시, DART corp_code 캐시와 대칭), `SecFinancialAdapter`(us-gaap 연간/분기 재무제표 3종 + 투자지표)
  - KIS 해외 시세: `KisStockPriceAdapter`가 `marketType.isDomestic()` 분기로 미국 현재가 조회 가능, 해외 종목 마스터 캐시 보유
  - 환율: `KoreaEximExchangeRateAdapter` (통화별 KRW 환산)
  - 도메인 모델: `Stock`/`MarketType`/`ExchangeCode`는 해외 전제 설계(거래소별 통화 내장)
  - 선례: 챗봇 `ChatContextBuilder`가 KR→DART / US→SEC 분기 기확립
- 문제: 리포트 조립 계층이 DART/한국 회계 전제로 결합 — 한글 계정명 매칭(`SnapshotFinancialExtractor` 863줄), `\d{6}` 종목코드 검증, `stock_code` VARCHAR(6), 원화 단위 가정(통화 필드 없음), 프론트 KRX 검색 필터

## 실측 검증 (2026-08-02, AAPL)

SEC companyfacts/submissions API 직접 호출로 확인:

- performance/statements/valuationInputs/priceMetrics/배당 전 항목 조달 가능 (매출 416.2B$, 영업이익 133.1B$, 현금 35.9B$, 차입금 장기 78.3B$, EPS 7.46$, DPS 1.02$ 등 FY2025 실측)
- 순이익 2009~2025 17년 보유 → 10년 타임라인 가능
- 기업 프로필: 회사명·SIC 업종·결산월·거래소·주소 제공. CEO명·웹사이트는 미제공
- **버그 발견**: AAPL의 `Revenues` 태그에 FY2018 1건만 존재(태그 세대교체 — 현행은 `RevenueFromContractWithCustomerExcludingAssessedTax`). `SecFinancialAdapter.getTagValues`(459-467행)는 "첫 번째 비어있지 않은 태그"를 채택하므로 연간 매출이 null로 표시됨
- 태그 함정: 배당총액도 `PaymentsOfDividendsCommonStock`(FY2017까지)→`PaymentsOfDividends`(현행) 세대교체. Goodwill·무형자산은 FY2017 이후 미공시(중요성 사유) → 항목 누락 허용 설계 필요
- 이익잉여금 음수(-14.3B$, 자사주 소각 영향) — ratios 판정 로직의 미국 케이스 대응 필요
- 증자 탐지: 애플의 S-3/424B2는 전부 채권 발행 → 서식 타입만으로 유상증자 판별 불가. 발행주식수 추이(155.5억→147.8억주)가 더 정확

## What We're Building

1. **SEC 어댑터 보강**: 태그 폴백 버그 수정(최근 연도 커버 우선 채택), 보관 이력 3년→10년(파라미터화), valuationInputs·배당·주식수(dei 택소노미)·submissions(기업 프로필/제출 서식) 조달 추가
2. **리포트 조립 확장**: 국가별 Assembler/Extractor 전략 분리 — US 전용 신규 작성, 기존 KR 로직(`SnapshotFinancialExtractor`) 불변. `ReportSnapshot`에 country/currency 추가(schemaVersion 2)
3. **제약 해제**: 종목코드 `\d{6}` 검증(요청 DTO·도메인 불변식·타임라인·공시 4곳)→티커 허용, `stock_code` VARCHAR(6) 확대, 프론트 KRX 검색 필터 해제, USD 표시 포맷($·B/M 단위) 분기
4. **해외 주가 히스토리**: KIS 해외 기간별시세 연동 (`StockPricePort.getPriceHistory()` 해외 기본 구현이 빈 리스트 → 월봉 차트 미동작 해소)

## 확정된 결정 (태형님, 2026-08-02)

| 항목 | 결정 |
|------|------|
| 1차 범위 | **미국(SEC EDGAR)만** — 일본·중국·홍콩 등 기타 해외 시장 제외 |
| 어댑터 매출 버그 | **본 확장 작업에 포함해 수정** — 별도 선행 이슈로 분리하지 않음 |
| 주주 동향 섹션 | **1차 미지원** — 미국은 구조화 API 부재(SC 13D/G·DEF 14A는 문서 파싱 필요). 후속 이슈로 분리 |
| 데이터 소스 | **SEC EDGAR(무료·기연동) + KIS 해외 시세 + 수출입은행 환율** — 상용 API(FMP·Finnhub 등) 미도입 |
| 진입점 UX | **단일 진입점** — 사전 한국/해외 선택 없이 기존 검색창 하나로 통합. 검색 결과에 거래소/국가 배지·통화 표시. KRX 필터 제거 (태형님 확정 2026-08-02) |
| 미지원 섹션 처리 | **3단계 처리** — 섹션 전체 미지원(주주 동향)은 화면에서 제거 + 리포트 상단 안내 배너 1곳에 명시 / 필드 단위 미제공(CEO명 등)은 "-" 표시 / 대체 데이터 존재 시 라벨 변경해 제공(공시→SEC 제출 서식, 유상증자→발행주식수 추이). 스냅샷에 미지원 섹션 목록(`unsupportedSections`)을 명시해 "데이터 없음(오류)"과 "미지원(설계)"을 구분 (태형님 확정 2026-08-02) |

## 구현 방향

- **어댑터 계층**: `SecFinancialAdapter` 폴백을 "최근 연도를 커버하는 태그 우선"으로 수정(기존 SEC 화면도 함께 수선됨). submissions API 클라이언트 추가(프로필·서식 목록)
- **조립 계층**: `CompanyReportSnapshotService` → 국가 판별 후 KR/US Assembler 위임. US Extractor는 us-gaap 태그 직접 매핑(한글 계정명 매칭 불필요). 챗봇 `ChatContextBuilder`의 KR/US 분기 패턴 준용
- **스냅샷 스키마**: country/currency 추가(schemaVersion 2). 기존 KR 스냅샷은 마이그레이션 없이 기본값 KR/KRW로 해석
- **증자 시그널**: 발행주식수(dei) 추이 기반 희석 탐지로 대체
- **프로필 공란 처리**: CEO명 등 미제공 필드는 "-" 표시
- **진입점 UI**: 기존 단일 검색창 유지, `company-report.js`의 KRX 필터 제거 + 결과에 거래소/국가 배지·통화 표시. 미국 종목 선택 시 preview/상세 상단에 미제공 섹션 안내 배너(`unsupportedSections` 기반). 주주 동향 섹션은 미국에서 렌더하지 않음
- Entity 수정(`CompanyAnalysisReportEntity.stock_code` 길이)·public API 변경·비즈니스 로직 변경은 Approval Gate 대상 → plan 단계에서 상세 설계 후 승인받고 진행

## 검토한 대안 (채택 안 함)

- **상용 API(FMP·Finnhub·EODHD 등) 도입** — 글로벌 표준화 데이터 제공하나 유료 전환 전제·외부 의존 추가. SEC 무료 API가 이미 연동돼 있어 불필요
- **기존 `SnapshotFinancialExtractor`에 us-gaap 매핑 추가(단일 Extractor)** — 863줄 코드에 국가 분기 삽입은 KR 회귀 리스크. 전략 분리로 KR 불변 유지
- **매출 버그 별도 선행 이슈 분리** — 어댑터 보강과 같은 파일·같은 함수 수정이라 함께 진행 (태형님 확정)
- **서식 타입(S-1/S-3/424B) 기반 유상증자 탐지** — 채권 발행과 구분 불가(AAPL 실측). 발행주식수 추이 기반으로 대체
- **미국 외 시장 동시 확장** — 시장별 공시 API·택소노미 상이로 범위 과대. 미국 우선

## Edge Cases

- 태그 세대교체 기업(AAPL 매출·배당) → 폴백 체인이 최근 연도 커버 태그 채택
- 특정 항목 미공시 기업(AAPL Goodwill FY2017 이후) → 항목 누락 허용, null 표시
- 이익잉여금 음수(자사주 소각) → ratios 판정이 오류로 처리하지 않도록 확인
- 금융주 등 태그 체계 상이 업종 → 폴백 확대 후에도 누락 가능, null 허용·검증 종목에 포함
- 회계연도 변경 기업의 분기 YTD 보정(Q4=FY−누적Q3) 엣지 케이스
- CIK 미존재 티커(상장폐지·OTC) → 기존 `SecApiException` 흐름 유지
- 기존 KR 리포트 스냅샷(schemaVersion 1) 조회 → country/currency 기본값 해석으로 하위 호환

## 범위 밖 (하지 않음)

- 주주 동향 섹션(최대주주 10년·5%룰) 미국 지원 — SC 13D/G·DEF 14A 파싱은 후속 이슈
- 미국 외 해외 시장(일본 EDINET·중국·홍콩 등)
- CEO명 등 SEC 미제공 프로필 필드 보강(공란 처리)
- 상용 재무 데이터 API 도입
- 기존 KR 리포트 로직·화면 동작 변경(스냅샷 스키마 필드 추가 제외)
- 챗봇·포트폴리오 등 타 기능의 해외 지원 확대
