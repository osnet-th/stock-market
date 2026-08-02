# 기업 분석 리포트 미국 주식 확장 게이트 로그

## 원칙
- 각 단계 시작 전 태형님에게 작업 내용을 제시한다.
- 다음 단계로 넘어갈지에 대한 판단은 태형님에게 넘긴다.
- 단계별 산출물은 이 게이트 로그를 `gate: docs/gates/2026-08-02-us-company-report-gates.md`로 참조한다.

## Stage Decisions
- start: approved
- brainstorm: approved (2026-08-02, 태형님 "어댑터 매출 버그도 지금 기능확장하면서 같이 수정하는쪽으로 진행하는걸로 해줘")
- issue: approved (2026-08-02, 동일 지시 — brainstorm→issue 진행안에 대한 승인)
- plan: approved (2026-08-02, 태형님 "진행해" — Approval Gate 대상 7건 일괄 승인)
- work: approved (2026-08-02, 태형님 "진행해" — review 진입 승인)
- review: approved (2026-08-02, 태형님 "진행해" — validation 진입 승인. L1·L3 수정 지시 반영, L2 후속 확정)
- validation: approved (2026-08-02, 태형님 로컬 8080 직접 테스트 후 "진행해" — 미검증 4건은 배포 후 확인으로 수용)
- commit: approved (2026-08-02, 태형님 "진행해 / main 까지 병합해줘" — docs/commits 기록)
- push: approved (2026-08-02, 동일 지시 — PR 생성·main 병합 포함, docs/pushes 기록)

## Stage Log
- start: 2026-08-02, 태형님 "기업 분석 리포트 해외 주식 지원 가능 여부·확장 방식·데이터 소스 확인" — 코드베이스 구조 조사(companyreport·DART·SEC·KIS 어댑터), 미국 기준으로 범위 축소 지시
- brainstorm: 완료 (2026-08-02, docs/brainstorms/2026-08-02-us-company-report-brainstorm.md)
  - SEC companyfacts/submissions API 실호출(AAPL)로 리포트 전 재무 섹션 조달 가능 검증, 순이익 17년 이력 확인
  - 실측 중 `SecFinancialAdapter.getTagValues` 태그 폴백 버그 발견(AAPL 매출 null) → 태형님 확정: 본 확장 작업에 포함해 수정
  - 확정 4건: 미국만 / 매출 버그 포함 / 주주 섹션 1차 미지원 / SEC+KIS+환율 무료 소스 유지
  - 추가 확정 2건 (2026-08-02, 태형님 "단일진입점으로 진행하고 거래소 배지랑 안내 배너로 표시하는걸로 진행해줘"): 진입점은 단일 검색(사전 시장 선택 없음, 거래소/국가 배지·통화 표시) / 미지원 섹션은 3단계 처리(섹션 제거+상단 안내 배너, 필드 "-", 대체 데이터 라벨 변경 제공, `unsupportedSections` 명시)
- issue: 완료 (2026-08-02, GitHub Issue #105 등록 — docs/issues/2026-08-02-us-company-report-issue.md 참조. worktree `feat/issue-105-us-company-report` 생성)
- plan: 완료 (2026-08-02, docs/plans/2026-08-02-001-feat-us-company-report-plan.md — 태형님 "진행해" 승인, Approval Gate 대상 7건 일괄 승인)
- work: 완료 (2026-08-02, docs/works/2026-08-02-us-company-report-work.md — Phase 1~4 구현, compileJava·jsc 구문 검증 PASS. plan 대비 편차 3건 기록. SEC/KIS 실호출·KR 회귀·UI 확인은 validation 항목)
- review: 완료 (2026-08-02, docs/reviews/2026-08-02-us-company-report-review.md — H1(10-K 내 Q4·비교치 행 오채택) 발견·수정·실데이터 재검증, M1(StockPort @Primary) 확인. 태형님 지시로 L1(툴팁 SEC 문구)·L3(월봉 차트 통화 라벨 재렌더) 추가 반영, L2(예상 매출 단위)는 후속 이슈로 수용. 재컴파일·jsc PASS)
- validation: 완료 (2026-08-02, docs/validations/2026-08-02-us-company-report-validation.md — gradlew test BUILD SUCCESSFUL(스프링 컨텍스트 부팅 포함), KR 회귀 정적 diff 확인, SEC 실호출 조립 AAPL/MSFT/JPM 검증, 프론트 헬퍼 하네스 21/22(1건은 하네스 기대값 오류). 미검증 4건: KIS 실호출(운영 토큰 무효화 리스크)·브라우저 렌더·KR 런타임 회귀·ddl 반영)

## Approval Gate 항목 (plan 문서에 상세 설계, plan 승인 시 일괄 승인 처리)
1. `ReportSnapshot` 스키마 변경 — country/currency/unsupportedSections 추가, schemaVersion 2
2. `CompanyAnalysisReportEntity.stock_code` 컬럼 길이 6→20 확대 (Entity 수정)
3. 종목코드 검증 완화 — 요청 DTO·도메인 불변식·타임라인/공시 서비스·price-history (public API 동작 변경)
4. 신규 공개 API — `GET /api/stocks/{ticker}/sec/filings` (US 공시 패널용)
5. 국가별 Assembler/Extractor 전략 분리 (구조 변경)
6. `SecFinancialAdapter` 폴백 로직 수정·이력 10년 확장 (기존 SEC 화면 동작 변경 — 버그 수정 방향)
7. 신규 외부 호출 — SEC submissions API, KIS 해외 기간별시세

## Notes
- 사전 조사 산출물: 조사는 본 세션 대화로 수행, AAPL 실측 스크립트는 세션 스크래치패드(임시)에서 실행 — 저장소 미포함
- worktree: /Users/tang/Documents/workspace/wt-issue-105-feat-issue-105-us-company-report (base: main 9579dea)
