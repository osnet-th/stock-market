---
title: "fix: 재무 타임라인 연도별 DART 계정 taxonomy 불일치 값 누락"
type: fix
status: active
date: 2026-07-19
origin: docs/brainstorms/2026-07-19-timeline-account-taxonomy-brainstorm.md
issue: https://github.com/osnet-th/stock-market/issues/85
gate: docs/gates/2026-07-19-timeline-account-taxonomy-gates.md
---

# fix: 재무 타임라인 계정 taxonomy 불일치

## Overview
현대차(005380) 등에서 연도별 DART 계정 표기가 해마다 달라(구 `ifrs_` vs 신 `ifrs-full_` 접두사, 주요계정 결측) 타임라인 조립 시 값이 "—"로 빠지는 문제를 **조립기(FinancialTimelineAssembler)** 에서 근본 수정한다. 타임라인은 기업 리포트·종목 평가·재무 타임라인이 공유하므로 세 곳 모두 정상화된다.

## Approval Gate (본 plan 승인으로 확정)
- 공유 컴포넌트 `FinancialTimelineAssembler`(stock.application)의 계정 병합/요약 로직 동작 변경 → 종목 평가·재무 타임라인·기업 리포트 스냅샷 영향. 회귀 검증(삼성 등) 필수.
- `SnapshotFinancialExtractor`의 매칭이 조립기 canonical id에 맞춰 동작하도록 동반 수정.

## 확정 원인 (원천 DART 실측, 현대차)
- **A 영업CF 2017·2018**: 세부계정 `id=ifrs_...`(≤2018) vs `ifrs-full_...`(≥2019) → `detailRowKey`(accountId 키잉)가 두 행으로 분리 → 추출기 `findFirst()`가 신 접두사 행만 선택.
- **B 당기순이익 2018~2022**: 주요계정(`fnlttSinglAcnt`)에 해당 연도 당기순이익 부재. 값은 전체재무제표 IS "연결당기순이익"에 존재(2018 1.65조·2019 3.19조·2020 1.93조·2021 5.69조·2022 7.98조; ≤2022 무표준코드, ≥2023 `ifrs-full_ProfitLoss`). 지배·비지배·계속영업 제외 시 연도당 총계 1개.

## 수정 설계

### Phase 1 — Bug A: IFRS 계정 id 접두사 정규화
- `canonicalAccountId(id)` 헬퍼 신설: `ifrs-full_` / `ifrs_` 접두사를 **동일 canonical**로 정규화(예: `ifrs-full_` → `ifrs_`). `null`·`-표준계정코드 미사용-`·`dart_`·기타는 이름 키 유지(기존 동작 보존).
- 적용: `FinancialTimelineAssembler.detailRowKey`가 canonical id로 키잉 → `ifrs_X`/`ifrs-full_X` 세부행 병합(BS/IS/CF 전체).
- 동반: `SnapshotFinancialExtractor.matches`/`detailSeries`, `FinancialTimelineAssembler.findCashFlowAmount`가 canonical id로 비교(상수는 `ifrs-full_...` 유지하되 비교 시 정규화).
- **결정 필요**: canonical 헬퍼 배치 위치(공유 util — stock.application 또는 공통 지점). extractor(companyreport)와 assembler(stock) 양쪽에서 동일 규칙 사용해야 하므로 단일 소스로 둔다.

### Phase 2 — Bug B: 당기순이익 총계 보강(요약 결측 시 상세에서 채움)
- 조립기에 **총계 당기순이익 시리즈** resolver 신설: 전체재무제표 IS에서 이름 정규화가 "당기순이익"/"당기순이익(손실)"/"연결당기순이익"이고 "지배기업소유주지분"·"비지배지분"·"계속영업"·"중단영업" **미포함**인 행의 연도별 값(id 우선 `ifrs-full_ProfitLoss`, 없으면 이름 규칙).
- 요약 당기순이익 행에 **결측 연도만 보강**(기존 값 우선 → 회귀 최소). 요약 자체가 없으면 상세 총계로 행 생성.
- **결정 필요**: (a) 결측 보강만 vs (b) 당기순이익 요약을 상세 총계로 일원화. 회귀 안전상 (a) 권장.

## Phase별 작업 (work 체크리스트)
- [ ] `canonicalAccountId` 헬퍼 + 배치 결정, 단위 동작 확인
- [ ] `FinancialTimelineAssembler.detailRowKey` canonical 적용
- [ ] `SnapshotFinancialExtractor.matches`/`detailSeries` canonical 비교 + `findCashFlowAmount` 동반
- [ ] 총계 당기순이익 resolver + 요약 결측 보강
- [ ] 컴파일

## 검증 계획 (validation)
- 앱 기동 후 현대차 005380 스냅샷: 영업CF·당기순이익·FCF **2017~2026 전부** 채워짐, 성장률/EPS 파생 정상.
- 회귀: 삼성전자 등 기존 정상 종목 스냅샷 값 불변(수정 전후 비교), 종목 평가 재무 타임라인 세부계정 트리 중복/누락 없음.

## 리스크
- id 정규화가 서로 다른 계정을 오병합할 위험 → `ifrs`/`ifrs-full` 접두사만 정규화(suffix 동일 = 동일 요소), `dart_`·무표준코드는 이름 키 유지로 한정.
- 당기순이익 총계 선택 시 지배/비지배/계속영업 라인 오선택 위험 → 제외 규칙 명시 + 연도별 실측 검증.
- 공유 타임라인 변경 → 종목 평가 회귀 반드시 확인.

## 범위 밖
- 영업CF 음수(정상 DART 값), #84.
