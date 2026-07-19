# 기업 리포트 UX 개선 2차 - Plan

**Date:** 2026-07-19
**Issue:** #88 (docs/issues/2026-07-19-company-report-ux-round2-issue.md)
**Gate:** docs/gates/2026-07-19-company-report-ux-round2-gates.md
**Brainstorm:** docs/brainstorms/2026-07-19-company-report-ux-round2-brainstorm.md (Status: Decided)

## 범위 확정
- 3작업, 1 worktree. 모두 프론트(`company-report.html` / `company-report.js`) 중심. **백엔드·Entity·API·저장 스키마 변경 없음.**
- 권장 구현 순서: Phase 3(가벼움) → Phase 2 → Phase 1(무거움). 문서 번호는 brainstorm과 맞춰 ①②③로 유지.

---

## Phase 1 (①) — 주가지표 자동/내 계산 통합 (재료 인라인 편집)

### 설계
"주가지표(자동)" 그리드 + "주가지표 계산기(내 계산)" 두 블록을 **하나의 카드/표**로 통합. 세 구역:

- **재료 (편집):** 주가 · 유통주식수 · 당기순이익 · 자본총계 · 매출액 · 영업CF — 인라인 입력. 각 칸의 placeholder에 **실제 자동값**을 숫자로 표시(비우면 자동값 사용). 음수 입력(적자·자본잠식) 유지.
- **파생 지표 (자동 계산):** 시가총액 · EPS · BPS · PER · PBR · PSR · PCFR · PER×PBR — `crCalcMetrics`/`crMetricBase` 그대로. 재료 편집 시 즉시 재계산. 계산식 + 대입값 병기(`crFormulaText`).
- **자동 전용 (편집 불가):** EV/EBITDA(근사) · ROIC(근사) · 발생액/총자산 — 스냅샷값만 표시(재료 매핑 없음).

### 저장 의미(무변경)
- `metricInputs`(v2: price/shares/netIncome/equity/revenue/operatingCf + legacy eps/bps) 그대로. **빈 칸 = 자동** 규칙 유지 → "데이터 새로고침" 시 자동값 갱신 정상, 사용자가 고친 칸만 override 저장.
- 자동값을 칸에 미리 채워 넣지 않음(prefill 안 함) — placeholder로 자동값을 보여줘 "처음엔 자동, 고치면 override" 규칙을 안전하게 유지.

### 표시 기준 (Open Q1·Q2 확정)
- 파생 EPS/BPS의 "자동 상태" 값은 기존 `_crPerShare` 우선순위(입력 → legacy → 자동 분자÷주식수 → 스냅샷 지표) 그대로 채택. 현재 계산기 동작과 동일.
- 자동 전용 3행은 통합 표 하단에 "자동 전용" 소구분으로 함께 배치(별도 카드 분리 안 함).

### 변경 위치
- 작성 5단계: `company-report.html:475-495`(자동 그리드) + `:496-617`(계산기) → 통합 표 1개로 재구성. 재료 입력 그리드(`:499-542`)를 표 안 인라인 입력으로 흡수. 예상매출 기반 지표 표(`:594-616`)는 유지.
- 상세 6번: `company-report.html:1197-1215`(자동 그리드) + `:1221-1250`(조건부 파란 "내 계산" 블록) → 통합 표 1개(읽기 전용, 저장된 override 반영한 **유효값** + 자동 전용 3행). `crHasCustomMetrics` 조건부 블록 제거·흡수.
- JS: 계산 엔진 재사용, UI 조립용 헬퍼만 소폭 추가 가능(예: 재료 행 메타 배열). 로직 변경 없음.

### 체크리스트
- [ ] 작성 5단계 통합 표 마크업 (재료 인라인 + 파생 + 자동 전용)
- [ ] 상세 6번 통합 표 마크업 (유효값 + 자동 전용, 조건부 블록 제거)
- [ ] 재료 placeholder에 자동값 숫자 표시, 빈칸=자동 규칙 확인
- [ ] 툴팁(`crHintShow`/`crBreakdownText`) 통합 표에서 정상 동작
- [ ] 구 스키마(legacy eps/bps) 리포트 열람 시 값 손실 없음 확인

---

## Phase 2 (②) — DART 10년 정기보고서 바로가기

### 설계 (프론트 전용)
- 기존 `API.getDisclosures(stockCode, from, to, ['A'])` 재사용. `from = 오늘−10년`, `to = 오늘`. 어댑터는 1페이지 100건 → 10년 정기공시(~40건) 단일 페이지 충분(확인됨, DartFinancialAdapter.java:80).
- `reportName` 필터: **사업보고서 / 분기보고서 / 반기보고서**만. `viewerUrl` 새 탭.
- 정렬: 접수일 최신순(백엔드 기본). 정정 보고서는 remark 마커 표기(재사용 `isCorrectionDisclosure` 유사).

### 노출 위치/형태/시점 (Open Q3 — 확정 제안, plan 승인 시 조정 가능)
- **위치:** 작성 위저드 최상단(스텝 인디케이터 아래, 전 단계 공통) + 상세 뷰 상단. 태형님 요청 "최상단"에 맞춤.
- **형태:** 연도별 그룹 접이식(기본 접힘), 각 연도에 사업/분기/반기 링크. 종목·연도 많아도 컴팩트.
- **시점:** 종목 선택 즉시 자동 로드(무거운 preview와 별개의 가벼운 호출).

### 변경 위치
- `company-report.js`: 상태 `companyReport.disclosures = { loading, error, byYear }` 추가. 메서드 `companyReportLoadDisclosures(stockCode)`, 헬퍼 `_crReportKind(reportName)`, `_crDisclosureFromDate()`. `companyReportSelectStock`·`_crEnterWizardFrom`·`companyReportOpenDetail`에서 호출.
- `company-report.html`: 위저드 상단 + 상세 상단에 링크 바 템플릿 추가.

### 체크리스트
- [ ] JS 상태·로드·필터·연도 그룹 헬퍼 추가
- [ ] 종목 선택/재개/상세 진입 시 로드 연결
- [ ] 위저드 상단 + 상세 상단 링크 바 마크업
- [ ] 10년 미만/공시 없음/오류 상태 처리

---

## Phase 3 (③) — 경쟁사 비교 "비교 내용" 여러 줄

### 변경 위치
- 편집(`company-report.html:369-375`): `note` `<input>` → `<textarea>`(rows 2, resize-y), 행 컨테이너 `items-center` → `items-start`.
- 상세(`company-report.html:1097`): note 셀에 `whitespace-pre-wrap` 추가.
- JS/백엔드 무변경(`_crBuildManual`의 `rows()` trim은 내부 줄바꿈 보존).

### 체크리스트
- [ ] 편집 textarea 전환 + 레이아웃
- [ ] 상세 줄바꿈 보존
- [ ] 기존 단일행 저장분 조회 정상

---

## Validation 계획
- `./gradlew compileJava` (프론트 정적파일 변경이라 컴파일 영향 없음 확인용) + 앱 기동 후 Browser pane 실동작:
  - ① 재료 편집 시 파생지표 재계산, 빈칸=자동, 자동 전용 3행 표시, 상세 단일 표.
  - ② 종목 선택 시 10년 사업/분기/반기 링크 렌더 + 원문 이동.
  - ③ 경쟁사 여러 줄 입력·저장·상세 줄바꿈.
  - 구 스키마(v1/legacy) 리포트 열람 하위호환.

## Approval Gates (work 진입 전 확인)
- ① 통합 표: 자동값 prefill 안 함(placeholder 표시) + 자동 전용 3행 동일 표 배치 — 표시 동작 변경.
- ② 링크 바: 작성+상세 상단 / 연도 그룹 접이식 / 종목 선택 시 자동 로드 — 위 제안대로 진행할지.

## 범위 밖
- 백엔드 스냅샷·주가지표 자동 산출 로직, 공시 백엔드/신규 API, `metricInputs` 저장 스키마, 투자판단·재무지표·청산가치·DCF 동작.
