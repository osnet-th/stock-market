# 기업분석리포트 Work 기록

gate: docs/gates/2026-07-12-company-analysis-report-gates.md
plan: docs/plans/2026-07-12-001-feat-company-analysis-report-plan.md
issue: https://github.com/osnet-th/stock-market/issues/81
branch: feat/issue-81-company-analysis-report (worktree)

## 진행 내역 (2026-07-12)

Phase 1~4 구현 완료. Phase 5(검증)는 validation 단계에서 수행.

### Phase 1 — stock 도메인 DART 확장

| 파일 | 내용 |
|------|------|
| `stock/domain/model/CompanyProfile.java` (신규) | 기업개황 도메인 record |
| `stock/domain/model/MajorShareholder.java` (신규) | 최대주주 현황 record (bsnsYear 포함) |
| `stock/domain/model/BulkHoldingReport.java` (신규) | 대량보유(5%룰) record |
| `stock/domain/service/CompanyDisclosurePort.java` (신규) | 신규 포트 (기존 StockFinancialPort 비대화 회피) |
| `stock/infrastructure/stock/dart/DartApiClient.java` (수정) | fetchCompanyProfile/fetchMajorShareholderStatus/fetchBulkHoldingReports 추가. company.json은 단일 객체 응답이라 전용 검증 경로 |
| `stock/infrastructure/stock/dart/dto/DartCompanyResponse.java` 외 2종 (신규) | 응답 DTO |
| `stock/infrastructure/stock/dart/DartCompanyAdapter.java` (신규) | 포트 구현 (corpCodeCache 재사용) |
| `stock/application/CompanyInfoService.java` (신규) | 기업개황 + 최대주주 10개년 병렬(실패 연도 skip) + 5%룰 |
| `stock/application/dto/CompanyProfileResponse.java` 외 2종 (신규) | application DTO |

### Phase 2 — 스냅샷 조립/계산 (companyreport/application)

- `dto/ReportSnapshot.java`: schemaVersion=1 스냅샷 record (columns/performance/statements/ratios/priceMetrics/valuationInputs/shareholders/riskSignals). 원시 데이터만 저장, 가치평가 결과 미저장.
- `dto/ReportValuation.java`: 파생 계산 결과 record (조회 시 계산).
- `SnapshotFinancialExtractor.java`: 타임라인 → 실적/재무제표/지표(기준치 판정 good·warn·risk)/주가지표(PSR·PCFR·PER×PBR·EV/EBITDA 근사·ROIC 근사·발생액/총자산)/청산가치 입력(IFRS 앵커 자식 분류 + 키워드 폴백, 미분류 목록)/위험 시그널 추출.
- `LiquidationValueCalculator.java` / `DcfCalculator.java`: 순수 계산 컴포넌트 (테스트 가능 구조).
- `CompanyReportSnapshotService.java`: 타임라인 10년(CFS→OFS 폴백) + 부가데이터 병렬 조합. 개별 항목 실패는 부분 스냅샷 허용. 최대주주 이력은 중첩 join으로 인한 executor 풀 고갈 방지를 위해 호출 스레드에서 실행.

### Phase 3 — Entity/CRUD

- domain: `CompanyAnalysisReport`(노트 6종·등급 7종·파라미터·스냅샷JSON), `ReportGrade`, `ReportNotes`, `InvestmentGrades(hasBuySignal)`, `ValuationParams(defaults)`, 포트, `CompanyReportNotFoundException`.
- infrastructure: `CompanyAnalysisReportEntity`(`company_analysis_report`, JSONB 2컬럼, 인덱스 user+stock / user+updated), JpaRepository, RepositoryImpl(userId 스코프 강제), Mapper + `ValuationParamsJsonConverter`(폐쇄형).
- application: `ReportSnapshotJsonMapper`(역직렬화 미지 필드 허용 — 스키마 진화 대비), Write/Read 서비스. create는 스냅샷 조립 실패에도 저장(이후 새로고침 보완), refresh는 실패 전파. 외부 API 호출은 트랜잭션 밖.
- presentation: Controller(7 엔드포인트), Request DTO(record, jakarta validation), ExceptionHandler(404/400/401/validation), SecurityContext 헬퍼.
- 시큐리티: prod `anyRequest().authenticated()`에 자동 포함 — 설정 변경 없음.

### Phase 4 — 프론트엔드

- `app.js`: validPages/menus/partialNames/navigateTo(case)/spread + 이탈 시 차트 정리 — 전부 추가만.
- `index.html`: 파티션 placeholder + script 태그. `_sidebar.html`: report 아이콘 2곳(모바일/데스크탑).
- `api.js`: company-reports 7메서드 (preview/create/refresh는 60s 타임아웃).
- `partials/company-report.html`: 목록(등급 칩 7 + 매수재료 배지 + 페이지네이션) / 작성·수정(종목 검색 → preview → 정성 6 textarea → 등급 7 select → 파라미터 토글) / 상세(9개 섹션).
- `js/components/company-report.js`: 상태 bag + CRUD + Chart.js 실적 차트(막대 금액(억) + 선 이익률(%), canvas `report-preview-perf`/`report-detail-perf`), 표시 헬퍼.

## 검증 상태 (컴파일/정적)

- `./gradlew compileJava` 통과 (기존 realestate deprecation 경고만).
- JS 3파일 JXA(osascript) 문법 검사 통과, 파티션 HTML 태그 균형 검사 통과.
- 런타임 검증(앱 기동 + preview/CRUD 실호출)은 validation 단계에서 수행 예정.

## 특이사항 / 결정

- 타임라인 10년: 백엔드 기지원(MAX_YEARS=10) 확인 → 스냅샷 서비스에서 years=10 호출만.
- 스냅샷 items: ACCOUNTS+FCF+DETAILS만 조회 (INDICES 4분류 제외 — 지표는 자체 계산으로 대체, 연 4콜 × 10년 절약).
- EV/EBITDA·ROIC·시가총액은 근사치로 라벨링, 결손 시 null("—").
- majorstock/elestock의 과거 이력 한계(약 2년)와 체결일 미제공은 화면에 명시 + DART 뷰어 링크로 대체.
- (2026-07-13 태형님 요청) 목록 필터를 종목명 부분일치(IgnoreCase) 전용으로 변경 — 포트/JPA/Impl/ReadService/Controller(`stockName` 파라미터)/api.js/컴포넌트/마크업 일괄 수정.
- (2026-07-14 태형님 요청) 정성 입력을 **항목별 구조화 입력**으로 개편: `ReportManual` 도메인 record(연혁/판매·매입처+분산평가/경쟁사/급변항목/주주이벤트 + 자유 메모 5종, 필드 500자·행 100개·메모 1만자 상한) → 노트 TEXT 6컬럼 제거, `manual` jsonb 1컬럼. 위저드에 행 추가/삭제 편집기, 상세는 표 렌더.
- (2026-07-14 태형님 요청) **예상 매출 입력**(연도 행 + 분기 4칸 + 선택 연간 예상 순이익, 단위 억/조 선택) — 실적 차트에 예상 연도 반투명 막대로 이어 그림. **주가지표 계산기**: 재료(주가/유통주식수/EPS/BPS/매출/영업CF, 자동값 폴백) 입력 → PER·PBR·PSR·PCFR·PER×PBR 즉시 계산(계산식 상시 표시), 예상 매출 기반 연도별 예상 PSR/PER. 저장은 manual JSONB 확장(Entity 컬럼 무변경). 연도 입력은 4자리 숫자 제한(프론트+도메인 검증). 판정 기준값 수정 기능은 보류.
- (2026-07-15 태형님 요청) 주가지표 카드 28곳(위저드 자동 11 + 상세 자동 11 + 내 계산 6)에 **"?" 툴팁 아이콘** 추가 — `crMetricHelp` 사전(계산식 + 데이터 출처 + "—" 결손 사유)을 native title로 표시.
- (2026-07-14 태형님 피드백) 위저드 UX 다듬기 3건: ① 1단계 로딩을 검색창 위 배너 → 검색창 아래 자리 안 카드로 이동 + 기업개황 카드 페이드인 + "준비 완료" 배지 ② 하단 버튼 표준 배치(이전 왼쪽·첫 단계 숨김 / 임시저장·다음·작성완료 오른쪽 끝, 미선택 시 "종목을 선택하세요" 표기) ③ 행 편집기 7종에 기본 빈 행 1개 시드(빈 행은 저장 시 자동 제외).
- (2026-07-13~14 태형님 요청) 작성 화면을 **7단계 위저드**로 개편 + **임시저장(draft/draft_step)**: Entity 컬럼 2개 추가, 도메인 normalizeDraftStep(2~7) 검증, 요청/응답에 draft·draftStep, 목록 "작성중·N단계" 배지 + 클릭 분기(draft→위저드 재개, 완성→상세), 완성 리포트 수정은 저장 버튼(위저드 2단계부터), 첫 임시저장 시 생성(스냅샷 1회 조립) 이후 빠른 갱신.
- (2026-07-15 태형님 요청·재작업) 주가지표 "?" 툴팁을 native `title` → **커스텀 hover/클릭 팝오버**로 교체(값이 안 보이던 문제). 이어 **계산 근거값(실제 대입값) 노출**: `PriceMetrics.breakdowns`(지표별 `terms`/`result`/`extras`) 백엔드 신설 + `SnapshotFinancialExtractor.buildBreakdowns`가 중간값(현재가·유통주식수·시총·매출·영업CF·순이익·자본총계·자산총계·영업이익·상각비·차입금·현금성·실효세율) 수집, 프론트 `crBreakdownText`가 "= 식 = 결과 (중간값)" 조립. 근거 결손 지표는 공식만, 상세 내계산 6카드는 근거 미표시. record 필드 추가 + `FAIL_ON_UNKNOWN_PROPERTIES=false`로 옛 스냅샷 하위호환.
- (2026-07-15 태형님 요청) 재무제표 요약 표를 하나로 합치지 않고 **재무상태표(BS)/손익계산서(IS)/현금흐름표(CF) 3구역**으로 분리. `statements`의 key 접두사(`bs.`/`is.`/`cf.`)로 프론트 그룹핑(`crStatementGroups`/`crStatementRows`), 표에 구역 소제목 행 삽입(연도 헤더 공유). 백엔드 무변경. 작성·상세 양쪽 적용.
- (2026-07-15 태형님 요청, 옵션 A) 주가지표 **EPS/PER 폴백**: 기존 `ValuationMetricService`가 계정명 정확일치(`"당기순이익".equals`)로 EPS를 못 구할 때(삼성전자 연결 계정명 표기 차이), `buildPriceMetrics`에서 timeline 당기순이익(account_id 매칭) ÷ 유통주식수로 EPS 보정 → PER·PER×PBR·breakdown 동반. 기존 `ValuationMetricService`·종목평가 화면 무변경.
