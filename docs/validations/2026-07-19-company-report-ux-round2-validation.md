# 기업 리포트 UX 개선 2차 - Validation

**Date:** 2026-07-20 (KST, work 세션 2026-07-19 연속)
**Issue:** #88
**Gate:** docs/gates/2026-07-19-company-report-ux-round2-gates.md

## 실행 명령/환경
- `./gradlew bootRun` (worktree, SPRING_PROFILES_ACTIVE=dev, SERVER_PORT=8082) — 빌드+부팅 성공: "Started StockMarketApplication in 4.272s" (compileJava 포함 성공).
  - PostgreSQL(5432) 연결 정상. Elasticsearch(9200) 다운 → 경고만, 크래시 없음(정상 degrade).
- dev 프로파일 = permitAll + anonymous. company-report **사용자 스코프 API(list/create/get/preview)는 `InsufficientAuthenticationException`(401)** → 로그인 없이 실데이터 불가.
- 대응: (a) **비-사용자스코프 공시 API는 실호출**, (b) ①③ 렌더는 **실제 partial + 실제 company-report.js + mock 스냅샷** 독립 하니스(`_harness.html`, 검증 후 삭제)로 검증.

## 검증 결과 (Browser pane)

### ② DART 정기보고서 바로가기 — 통과
- **실 API** `GET /api/stocks/005930/disclosures?fromDate=20150101&toDate=20260720&types=A` → **200**, 정기보고서 반환: `분기보고서 (2026.03)`, `사업보고서 (2025.12)` + `viewerUrl`(DART 원문). `_crReportKind`/`_crReportPeriod` 파싱 대상 확인.
- 렌더(하니스): 연도 그룹(2025→2024 desc), 라벨 `분기 1Q`/`분기 3Q`/`반기`/`사업`, `[기재정정]` 정정 배지 표시, 원문 새 탭 링크.

### ① 주가지표 자동/내 계산 통합 — 통과
- 구 마커 제거 확인: "주가지표 (자동)"·"주가지표 계산기 (내 계산)"·"내 계산 (입력 재료 기반" 모두 미존재.
- 작성 5단계 통합 표: 재료 6행이 **자동값 기본 표시**(주가 75,000원·유통 60.0억주·당기순이익 34조 등), 파생 8행 정확(시총 450조, EPS 5,667=34조÷60억, BPS 55,000, PER 13.24배, PBR 1.36배, PSR 1.8배, PCFR 8.04배, PER×PBR 18.05), 자동 전용 3행(EV/EBITDA 6.5·ROIC 12.3%·발생액 -2.1%).
- **재료 편집→즉시 재계산**: 유통주식수 override 30억주 → EPS 11,333·PER 6.62배·BPS 110,000·시총 225조로 연쇄 재계산 확인.
- 상세 6번 통합 표: 조건부 파란 "내 계산" 블록 제거, `detail.manual`(빈값→자동 60억주) 사용해 EPS 5,667·시총 450조, 가운데 열 자동값 안내("자동 60.0억주", F4 포맷 통일 반영).

### ③ 경쟁사 비교 여러 줄 — 통과
- 편집(3단계): `비교 내용`이 `<textarea>`(placeholder "여러 줄 입력 가능"), 행 컨테이너 `items-start`, 3줄 입력 반영.
- 상세: 경쟁사 note 셀 `white-space: pre-wrap`, 줄바꿈 3줄 보존 확인.

### 리뷰 반영(F1·F2·F4·F5) 확인
- F4: 자동 안내값 "자동 60.0억주"(주 접미) 형식 통일 렌더 확인. F5: crMaterialAutoHint/crAutoMaterials manual 제거 후 호출부 12곳 정상 동작.
- F1(gen 증가)·F2(reportName 정정 병행)는 코드 반영 확인(런타임 회귀 없음).

## 콘솔/에러
- 신규 Alpine 표현식 평가 에러 **없음**(crMaterialValue/crMaterialAutoHint/crCalcMetrics/crMetricBase/crFormulaText/crDisclosureByYear/crReportKindLabel 전부 클린).
- 잔여 콘솔 500(프로필/관심)은 이전 앱셸 로드 로그, 본 변경과 무관.
- 하니스 한정 `_formatYmd is not a function`: 하니스가 FinancialComponent 미로딩 탓. 실제 앱은 app.js가 전 컴포넌트를 한 Alpine 루트로 병합(financial.js에 `_formatYmd`(3)·`isCorrectionDisclosure`(1) 정의 확인) → 프로덕션 안전.

## 미검증/제약
- dev 인증 제약으로 **실제 preview 스냅샷 기반 ① 실데이터**와 **리포트 저장/재조회(user-scoped)**는 미검증. → 실계정 로그인 환경(스테이징/로컬 로그인)에서 최종 확인 권장.
- ② 10년 범위 100건 초과(정정 다수) 시 첫 페이지만 조회되는 어댑터 제약은 설계상 수용(정기공시 ~40건).

## 정리
- 검증용 `_harness.html`(src/build) 삭제, 8082 검증 인스턴스 종료. 변경 잔존: company-report.js·company-report.html 2파일.
