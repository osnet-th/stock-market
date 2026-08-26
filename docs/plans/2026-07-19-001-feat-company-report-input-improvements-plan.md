---
title: "feat: 기업 리포트 입력 개선 — 연혁 여러줄·년월, 매입처 품목, 주가지표 공식형 재계산"
type: feat
status: active
date: 2026-07-19
origin: docs/brainstorms/2026-07-19-company-report-input-improvements-brainstorm.md
issue: https://github.com/osnet-th/stock-market/issues/84
gate: docs/gates/2026-07-19-company-report-input-improvements-gates.md
---

# feat: 기업 리포트 입력 개선

## Overview

기업분석리포트(#81)의 정성 입력과 주가지표 계산기 불편함을 개선한다. 네 항목:
① 회사 연혁 내용을 여러 줄 입력, ② 연혁 날짜를 년 또는 년-월까지, ③ 매입처에 품목(원자재) 칸 추가, ④ 주가지표 계산기를 파생지표 전체 공식형(분자÷분모)으로 바꿔 리프 값만 고치면 연쇄 재계산.

`ReportManual`(record + 검증)과 프론트(`company-report.js`/`company-report.html`)만 대상이며, 신규 API·DB 테이블·백엔드 스냅샷 산출 로직 변경은 없다. `manual`은 JSONB 컬럼이라 record 필드 추가는 스키마 마이그레이션 없이 하위호환된다.

## Approval Gate 결정안 (본 plan 승인으로 확정)

### Gate 1. 매입처 품목 칸 — (권장) A: 공유 필드 추가

- `PartnerItem`은 판매처(customers)·매입처(suppliers) 공유 record. `item`(품목) 필드를 **추가**하되 라벨만 문맥에 맞게: 판매처=제품/품목, 매입처=매입품목(원자재).
- 근거: record 분리보다 단순하고, 판매처 품목도 유용. 선택 입력(null 허용)이라 기존 저장 리포트는 빈 값으로 정상 표시.
- 대안 B(매입처 전용): 판매처엔 숨김. 태형님이 B를 원하면 UI 노출만 매입처로 제한(record는 동일).
- record 변경: `PartnerItem(name, share, note)` → `PartnerItem(name, item, share, note)`.

### Gate 2. 연혁 날짜 검증 완화 — 연혁 전용 `YYYY` 또는 `YYYY.MM`

- 연혁 `year`만 `YYYY`(4자리) 또는 `YYYY.MM`(월 01~12) 허용. 구분자는 `.`로 통일(예: `2024`, `2024.03`).
- `financialChanges`·`revenueForecasts`의 연도는 기존 `\d{4}` **유지** → `requireYear`를 공유하지 않도록 연혁 전용 `requireYearOrMonth` 신설.
- 정렬(`crHistoryAsc`, localeCompare 문자열 비교)은 `2024` < `2024.03` < `2025`로 올바르게 동작(검증 완료).

### Gate 3. 주가지표 공식형 재계산 — 리프 입력 + 파생 산출, 하위호환

- `MetricInputs`에 **당기순이익(netIncome)·자본총계(equity)** 를 추가(금액 단위=amountUnit). 기존 `eps`·`bps`는 **레거시 폴백용으로 유지**(신규 UI 미노출).
  - record: `MetricInputs(price, shares, revenue, operatingCf, netIncome, equity, eps, bps)`.
- 프론트 산출 우선순위(`crMetricBase`):
  - `EPS = netIncome≠null ? (netIncome×unit) ÷ shares : (mi.eps ?? pm.eps)`
  - `BPS = equity≠null ? (equity×unit) ÷ shares : (mi.bps ?? pm.bps)`
  - 시가총액 = price × shares → PER=price÷EPS, PBR=price÷BPS, PSR=시총÷revenue, PCFR=시총÷operatingCf, PER×PBR.
- 자동 폴백값(백엔드 무변경): netIncome=스냅샷 performance `netIncome`행, equity=`priceMetrics.bps` breakdown의 "자본총계" term(없으면 `pm.bps × shares`).
- 하위호환: 필드가 모두 선택(추가)이라 v1 JSON(eps/bps만 있음)도 그대로 읽혀 폴백으로 사용. 데이터 마이그레이션 불필요. `schemaVersion`은 신규 저장분을 2로 기록(읽기는 버전 무관 관대 처리), 강제 이관 없음.
- UI: 계산기의 EPS/BPS 직접입력 칸 → **당기순이익/자본총계** 입력으로 교체. "내 계산" 표를 **공식 + 실제 대입값**으로 표시(예: `EPS = 당기순이익 ÷ 유통주식수 = 5,300 억 ÷ 683,000,000 = 7,757`). 리프만 고치면 즉시 재계산.

## Phase별 작업 (work 체크리스트)

### Phase 1 — 연혁: 여러 줄 + 년/년-월
- [ ] `ReportManual.java`: 연혁 전용 `requireYearOrMonth` 추가, `validateListFields`에서 연혁 year만 신규 검증 적용(급변·예상매출은 `requireYear` 유지).
- [ ] `company-report.html`(작성): 연혁 content `<input>` → `<textarea>`(rows 2). year 입력 마스크를 `YYYY`/`YYYY.MM` 허용(maxlength 7, 숫자+`.` 1개)으로 완화, placeholder 갱신.
- [ ] `company-report.html`(조회 `crHistoryAsc` 렌더): content `<td>`에 `whitespace-pre-wrap` 적용(줄바꿈 보존).
- [ ] 필요 시 `FIELD_MAX_LENGTH`(500) 여러 줄 수용 여부 점검(유지 예정).

### Phase 2 — 매입처 품목 칸 (Gate 1 확정안 반영)
- [ ] `ReportManual.java`: `PartnerItem`에 `item` 필드 추가, `requireFieldsWithin` 대상에 포함(customers·suppliers).
- [ ] `company-report.js`: `_crResetForm`·`_crSeedEmptyRows`의 customers/suppliers 시드에 `item:''` 추가(_crBuildManual은 `Object.entries` 순회라 자동 포함).
- [ ] `company-report.html`(작성): 판매처·매입처 행에 품목 `<input>` 추가(라벨/placeholder 문맥별: 판매처=제품, 매입처=원자재).
- [ ] `company-report.html`(조회): 판매처·매입처 표에 "품목" 열 추가.

### Phase 3 — 주가지표 공식형 (Gate 3 확정안 반영)
- [ ] `ReportManual.java`: `MetricInputs`에 `netIncome`·`equity` 추가, `empty()` 생성자 갱신, `validateUnitAndMetricInputs`의 `requireAmounts`에 두 필드 포함.
- [ ] `company-report.js`: `_crResetForm`(412)·초기 상태(49) metricInputs에 `netIncome:''`·`equity:''` 추가, `_crBuildManual`(511) 직렬화에 추가(+`schemaVersion` 2), `crMetricBase` EPS/BPS 산출 우선순위 교체 + equity/netIncome 자동 폴백.
- [ ] `company-report.html`(계산기): EPS·BPS 입력 칸 → 당기순이익·자본총계 입력으로 교체, "내 계산" 표에 EPS·BPS 행 + 대입값(공식 형태) 표시.
- [ ] 조회 "내 계산" 블록(`crHasCustomMetrics` 노출)도 동일 산식/표시 반영.
- [ ] 구 스키마(v1, eps/bps만) 리포트 조회·재편집 시 값 손실 없음 확인.

## 검증 계획 (validation 단계)
- 컴파일: `./gradlew compileJava`.
- 앱 기동 후 Chrome 확장으로 실동작:
  - 연혁: 여러 줄 입력·저장·조회 줄바꿈, `2024`/`2024.03` 저장·정렬, 잘못된 월(`2024.13`) 거부.
  - 매입처: 품목 입력·저장·조회 열 표시, 기존(품목 없는) 리포트 정상.
  - 주가지표: 유통주식수만 바꿔 EPS·PER·PBR 등 연쇄 재계산, 당기순이익/자본총계 입력 반영, 빈 값 자동 폴백, v1 리포트 폴백 표시.

## 리스크
- Jackson record 역직렬화: v1 JSON(신규 필드 없음) → null 매핑, 신규 JSON(레거시 필드 없음) 처리. `ReportManualJsonConverter`/ObjectMapper의 미지 필드·누락 생성자 인자 관대 처리 여부를 work 착수 시 확인(FAIL_ON_UNKNOWN/누락 처리).
- `PartnerItem` 필드 순서 변경 시 프론트 행 편집(x-model row.item) 매핑 정합성.
- 계산기 단위(억/조) 환산: netIncome/equity는 amountUnit, EPS/BPS는 원/주 — 환산 일관성.

## 범위 밖
- 투자판단·재무지표·청산가치·DCF 등 다른 섹션 동작 변경.
- 판매처/매입처 자동 수집, 백엔드 스냅샷 산출 로직(`SnapshotFinancialExtractor`) 변경.
- 신규 API·DB 테이블·Entity 컬럼 추가(모두 `manual` JSONB 내 처리).
