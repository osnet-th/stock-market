---
date: 2026-05-30
type: fix
status: completed
completed: 2026-05-31
topic: financial-detail-explicit-query
origin: docs/brainstorms/2026-05-30-financial-detail-explicit-query-requirements.md
---

# fix: 재무상세(KR) 명시적 [조회] 버튼 방식 전환

## Problem Frame

포트폴리오 화면에서 국내(KR) 종목 재무상세 조회 시 두 가지 문제가 있다.

1. **표시값/조회값 불일치**: 연도 셀렉트박스는 `financialYear` 초기값(2026)으로 보이지만, 종목 오픈 시 `getDefaultYear()`(2025)로 덮어쓰고, 추가로 `shouldFallbackFinancialYear`가 무음으로 전년도 재조회하며 셀렉트값을 변경한다. 사용자가 보는 값과 조회되는 값이 어긋난다.
2. **자동 조회로 통제 부족**: 메뉴 탭 클릭(`selectFinancialMenu`)·셀렉트 변경(`onFinancialFilterChange`, 4개)이 즉시 `loadSelectedFinancial()`를 호출한다. 사용자가 원하는 연도/보고서타입을 먼저 지정하고 조회하기 어렵다.

KR만 대상으로, 데이터 조회는 오직 [조회] 버튼으로만 발생하도록 트리거 방식을 바꾼다. 백엔드 API 시그니처(year, reportCode)와 SEC 동작은 변경하지 않는다.

## Requirements Trace

| Req | 내용 | 해결 구현 단위 |
|-----|------|----------------|
| R1 | 메뉴 탭 클릭은 메뉴 전환만, 자동 조회 없음 | U1 |
| R2 | 종목 첫 오픈 시 자동 조회 없음 | U1 |
| R3 | 4개 셀렉트(연도/보고서타입/지표분류/재무제표구분) `@change` 자동 조회 제거 | U2 |
| R4 | [조회] 버튼 클릭 시 현재 메뉴+연도+보고서타입으로 조회 | U2 |
| R5 | 표시 연도 == 조회 연도 항상 일치 (폴백 제거, init 정렬) | U3 |
| R6 | 메뉴 전환해도 연도/보고서타입 유지 (전역 상태 유지) | U1 |
| R7 | 미조회 메뉴는 빈 상태 안내 표시 | U4 |

## Key Decisions (origin 계승 + 계획 확정)

- **조회 트리거**: 탭 클릭/셀렉트 변경 자동 조회 모두 제거, 명시적 [조회] 버튼만. (see origin)
- **적용 범위**: KR만. SEC는 탭클릭 자동조회 유지(의도적 비대칭 수용). (see origin)
- **연도 폴백 제거**: `shouldFallbackFinancialYear` 및 호출부 제거. 선택 연도 그대로 조회, 없으면 빈 결과 안내. (see origin)
- **상태 범위 (Deferred 해소)**: 4개 모두 **전역 1개 유지**. 단 근거를 분리한다 — `financialYear`/`financialReportCode`는 R6(탭 전환 시 연도/보고서타입 유지)를 **직접 충족**한다. `financialIndexClass`(indices 전용)/`financialFsDiv`(full-statements 전용)는 R6 범위 밖이며, 각 전용 메뉴에서만 사용되므로 전역 유지가 **무해**(다른 메뉴 조회에 영향 없음)하여 그대로 둔다. 메뉴별 분리는 과설계.
- **미조회 센티넬 (Deferred 해소)**: `financialResult`의 `null`=미조회, `[]`=조회했으나 0건으로 구분. 세 빈 상태 = ① 메뉴 미선택(`selectedFinancialMenu===null`) ② 메뉴 선택했으나 미조회(`selectedFinancialMenu!==null && financialResult===null`) ③ 조회 후 0건(`financialResult.length===0`). 각각 별도 문구.
- **탭 전환 시 결과 처리 (Deferred 해소)**: 메뉴 전환 시 `financialResult=null`로 즉시 초기화(미조회 상태로 리셋). 다른 메뉴의 이전 결과가 새 메뉴에 표시되는 혼동 방지. 연도/보고서타입 선택값은 유지(R6).
- **[조회] 버튼 사양 (Deferred 해소)**: 파라미터 셀렉트 행 끝에 배치. 레이블 "조회". `financialLoading` 중 disabled. 보고서타입 옵션 미로드(`financialOptions` 비어있음) 시 disabled.
- **KR 에러 상태 (Deferred 해소)**: 신규 전역 상태 `financialError`(string|null, 단일 string만 — 에러 타입 분기/재시도 카운터 등 추가 추상화 금지) 추가. 조회 실패 시 메시지 표시 + [조회]로 재시도. 기존 `secFinancialError`는 SEC 전용이라 재사용 안 함. **에러 시 `financialResult`는 `null`로 둔다**(기존 catch의 `[]` 설정 변경) — `[]`로 두면 0건 빈 상태(③)와 에러가 동시 노출되어 센티넬이 깨지므로.
- **reportCode 옵션 메뉴 불변성 (Deferred 해소)**: 옵션은 `loadFinancialOptions()`로 종목 단위 1회 로드, 메뉴별로 재호출하지 않음 → 메뉴 전환 시 전역 `financialReportCode` 그대로 유효. 별도 리셋 불필요.
- **옵션 로드 실패 복구 (adversarial 지적)**: 현재 `loadFinancialOptions` 실패 시 `financialOptions`가 null로 남고 throw하지 않는다. 이 경우 [조회] disabled가 영구화되어 lawsuits 외 모든 KR 조회가 막힌다. → `runFinancialQuery()`에서 `financialOptions`가 null이면 `loadFinancialOptions()`를 1회 재시도한 뒤 조회를 진행한다(막다른 상태 방지).
- **폴백 제거의 빈 결과 영향 (product/adversarial 지적)**: 폴백 제거는 사용자 확정 사항(see origin line 43)이나, 전년도 연차보고서 미공시 종목에서는 첫 [조회]가 0건이 될 수 있다. 이는 R5 보장을 위한 의도적 트레이드오프로 수용하며, ③ 빈 결과 문구가 '다른 연도를 선택' 행동을 명확히 유도하도록 한다(U4).

## Scope Boundaries

- SEC(해외) 분기/연간 토글 및 자동조회 동작 미변경.
- 백엔드 API 시그니처(year, reportCode) 미변경. 프론트 트리거만 변경.
- lawsuits는 보고서타입 미사용, 연도 셀렉트 공유(`getLawsuits(code, year+'0101', year+'1231')`). 자동조회 제거 원칙 동일 적용, [조회] 버튼으로 조회.

## Files

- `src/main/resources/static/js/components/financial.js` — 트리거 함수 수정, 폴백 제거, 조회 핸들러 추가
- `src/main/resources/static/js/components/portfolio.js` — 상태 초기화 정렬, `financialError` 추가
- `src/main/resources/static/partials/portfolio-deposit-financial.html` — `@change` 제거, [조회] 버튼 추가, 빈 상태/에러 템플릿

## Implementation Units

### U1. 탭 클릭 자동 조회 제거 + 첫 오픈 자동 조회 제거 (R1, R2, R6)
**파일**: `financial.js`

- [x] `selectFinancialMenu(menuKey)` KR 분기에서 `await loadSelectedFinancial()` 호출 제거. 메뉴 설정 + `financialResult=null`(미조회 리셋) + `financialError=null`까지만 수행. SEC 분기는 그대로 유지.
- [x] 필터 리셋 로직: 현재 `selectFinancialMenu`는 `financialAccountFsFilter`/`financialStatementFilter`만 `''`로 리셋하고 `financialIndexClass`/`financialFsDiv`는 **건드리지 않는다**. 이 기존 동작을 그대로 유지한다(전역값 유지, 추가 리셋 없음). 연도/보고서타입(`financialYear`/`financialReportCode`)도 리셋하지 않음(R6 유지).
- [x] `openStockDetail(item)` KR 분기는 `selectedFinancialMenu=null, financialResult=null` 유지(이미 자동조회 없음). `financialError=null` 추가.

**테스트 시나리오**:
- 메뉴 탭 클릭 시 네트워크 요청이 발생하지 않는다(자동조회 없음).
- 메뉴 A 조회 후 메뉴 B로 전환하면 결과가 미조회 상태(`financialResult===null`)로 리셋된다.
- 메뉴 전환 후에도 연도/보고서타입 선택값이 유지된다.

### U2. 4개 `@change` 제거 + [조회] 버튼 추가 (R3, R4)
**파일**: `portfolio-deposit-financial.html`, `financial.js`

- [x] HTML: 연도(326)/보고서타입(338)/지표분류(350)/재무제표구분(362) 셀렉트의 `@change="onFinancialFilterChange()"` 제거. `x-model` 바인딩은 유지.
- [x] HTML: 파라미터 행(`flex flex-wrap gap-3`, line 321) 끝에 [조회] 버튼 추가. `@click="runFinancialQuery()"`, `:disabled="portfolio.financialLoading || !hasFinancialOptions()"`. 기존 로딩 스피너(line 369-377)와의 관계 명시: 버튼은 스피너 **앞**에 배치하고, 좁은 폭 wrap 시에도 셀렉트 그룹과 분리되지 않도록 행 끝 고정. disabled 시각 처리는 기존 버튼 컨벤션(opacity + cursor-not-allowed) 따름.
- [x] `financial.js`: `hasFinancialOptions()` 계약 명시 — `if (selectedFinancialMenu === 'lawsuits') return true;`(보고서타입 불필요) `return !!portfolio.financialOptions;`(기존 보고서 셀렉트 게이팅 line 335와 동일 기준).
- [x] `financial.js`: `onFinancialFilterChange()` 호출부(HTML 4곳) 제거 후 함수 삭제. **선결**: 정적 자산 전체 grep으로 다른 참조 없음 확인.
- [x] `financial.js`: `runFinancialQuery()` 추가 — `selectedFinancialMenu`가 없으면 무시. `financialOptions`가 null이면 `loadFinancialOptions()` 1회 재시도(옵션 실패 복구). 이후 `loadSelectedFinancial()` 호출.

**테스트 시나리오**:
- 셀렉트값만 변경하면 네트워크 요청이 발생하지 않는다(4개 모두).
- [조회] 클릭 시 현재 메뉴+연도+보고서타입으로 1회 요청한다.
- 로딩 중에는 [조회] 버튼이 disabled 된다.
- 보고서타입 옵션 미로드 시 [조회]가 disabled (단, lawsuits는 조회 가능).
- 옵션 로드 실패 종목에서 [조회] 시 옵션 재시도 후 조회되어 영구 disabled에 빠지지 않는다.

### U3. 연도 폴백 제거 + 초기값 정렬 (R5)
**파일**: `financial.js`, `portfolio.js`

- [x] `loadSelectedFinancial()`에서 `shouldFallbackFinancialYear` 분기(재조회 + `financialYear` 변경) 제거. 단일 `fetchSelectedFinancial` 결과만 사용.
- [x] `shouldFallbackFinancialYear(result, year)` 함수 삭제.
- [x] `portfolio.js` `financialYear` 초기값을 `getDefaultYear()`와 동일 기준(전년도=2025)으로 정렬. 현재 `String(new Date().getFullYear())`(2026) → `String(new Date().getFullYear() - 1)`. (표시값과 첫 조회 연도 일치)
- [x] `openStockDetail`의 `financialYear=getDefaultYear()` 설정과 초기값이 동일 기준임을 확인(중복이지만 일관).

**테스트 시나리오**:
- 셀렉트에 보이는 연도와 조회 결과 연도가 항상 일치한다(무음 폴백 없음).
- 선택 연도에 데이터가 없으면 전년도로 자동 전환하지 않고 빈 결과 안내를 표시한다.
- 종목 첫 오픈 시 셀렉트값(2025)과 조회 대상 연도가 일치한다.

### U4. 미조회 빈 상태 센티넬 + 문구 분리 (R7)
**파일**: `portfolio-deposit-financial.html`

- [x] 게이팅 구조 주의: 기존 결과 영역(line 390)은 `x-if="financialResult !== null && !financialLoading"`로 `null`이면 렌더되지 않는다. ② 미조회 문구는 이 블록 **밖의 별도 형제 블록**으로 신설한다(아래 ② 조건). ①은 기존 line 543 영역 재사용.
- [x] 세 빈 상태 분리(상호 배타):
  - ① 메뉴 미선택: `selectedFinancialMenu === null` → "재무 메뉴를 선택하세요"(기존 라인 543 영역 재사용).
  - ② 메뉴 선택·미조회: `selectedFinancialMenu !== null && financialResult === null && !financialLoading && !financialError` → "조회 버튼을 눌러 데이터를 조회하세요"(신규 형제 블록).
  - ③ 조회 후 0건: `financialResult !== null && financialResult.length === 0 && !financialError` → 기존 "해당 연도/보고서의 데이터가 아직 등록되지 않았습니다… 다른 연도나 보고서를 선택해 주세요". (`!financialError`로 에러와 상호배타)
- [x] 결과 렌더 게이팅: `financialResult !== null && financialResult.length > 0 && !financialLoading`.

**테스트 시나리오**:
- 메뉴 미선택 시 ① 문구.
- 메뉴 선택 후 [조회] 전 ② 문구.
- 조회 결과 0건 시 ③ 문구.
- 위 세 상태가 동시에 겹치지 않는다(상호 배타적 게이팅).

### U5. KR 조회 에러 상태 + lawsuits 조회 동선 (R4)
**파일**: `portfolio.js`, `financial.js`, `portfolio-deposit-financial.html`

- [x] `portfolio.js`: `financialError: null` 상태 추가(U3의 portfolio.js init 변경과 동일 패스에서 추가).
- [x] `financial.js` `loadSelectedFinancial()`: try/catch로 실패 시 `financialError=메시지` + **`financialResult=null`로 설정**(기존 catch의 `financialResult=[]`를 변경 — 0건/에러 동시노출 방지). 성공 시 `financialError=null`. 기존 generation guard·차트 렌더링 로직 유지.
- [x] HTML: 에러 표시 블록 추가 `x-show="portfolio.financialError"` → 메시지 + [조회] 재시도 안내. 위치는 파라미터 행 바로 아래·결과 영역 위. 스타일은 기존 SEC 에러 블록(line 250 `bg-amber-50 border-amber-200`) 패턴 재사용(시각 일관성). SEC `secFinancialError`와는 별개 상태.
- [x] lawsuits: `runFinancialQuery()` → `loadSelectedFinancial()` → `fetchSelectedFinancial` lawsuits 분기가 연도 셀렉트값으로 `getLawsuits(code, year+'0101', year+'1231')` 호출함을 확인(기존 로직 유지, 트리거만 [조회]).

**테스트 시나리오**:
- KR 조회 실패 시 에러 메시지가 표시되고 [조회]로 재시도 가능.
- 에러 상태에서 메뉴 전환/재조회 시 `financialError`가 초기화된다.
- lawsuits 메뉴에서 [조회] 시 연도 셀렉트값 기준 기간으로 조회된다(보고서타입 미사용).

## Sequencing

U3(폴백 제거·init 정렬) → U1(탭 자동조회 제거) → U2(@change 제거·[조회] 버튼) → U4(빈 상태) → U5(에러·lawsuits).

- **U2 선결 게이트**: U2는 `loadSelectedFinancial()`를 호출하므로, U1(자동 호출부 제거)·U3(폴백 제거)이 **완료·검증된 후**에만 착수한다. U1/U3 미완 상태에서 U2 병행 시 폴백 잔존 등 일관성 깨진 조회 경로에 버튼이 연결될 위험.
- **U3 단독 가치**: U3(표시값=조회값 일치)는 동작 변경 없이 단독으로 사용자 가치를 전달한다. 동작 변경(U1/U2/U4/U5) 검증이 지연될 경우 U3만 선행 배포 가능.

## Dependencies / Assumptions

- Alpine.js x-model/x-if/x-show 패턴 유지, 빌드 프레임워크 없음.
- 보고서타입 옵션은 `/api/stocks/financial/options`에서 종목 단위 동적 로드(기존 유지).
- `FinancialComponent`는 무상태, 상태는 `PortfolioComponent.portfolio`에 집중(기존 패턴 유지).

## Risks

- 게이팅 조건 누락 시 빈 상태 중복 노출 → U4에서 상호 배타 조건(②③에 `!financialError`) 명시로 해소.
- `financialResult` null/[]/값 3분기를 모든 렌더 경로에서 일관 적용 필요(차트 렌더 finally 포함). 에러 경로는 `null`로 통일(U5).
- lawsuits는 보고서타입 셀렉트가 숨겨지므로 `hasFinancialOptions()`에서 `true` 반환으로 처리(U2 계약).
- 차트 잔존: `selectFinancialMenu`는 현재 `_secChartInstance`만 destroy하고 accounts용 `financialChartInstance`는 정리하지 않는다. 메뉴 전환→복귀 시 미조회(②) 상태인데 이전 차트 캔버스가 남을 수 있음. U1에서 메뉴 전환 시 `financialChartInstance`도 destroy하거나, 차트 컨테이너가 `financialResult!==null && length>0` 게이팅에 묶여 DOM 제거됨을 U4 검증에 포함.

## Verification

- 탭 클릭/셀렉트 변경 시 네트워크 무발생, [조회]에서만 요청(preview_network).
- 셀렉트 표시 연도 == 응답 연도.
- 미조회/0건/에러 문구 분기 동작.

## 구현 편차 (2026-05-31)

- **연도 기본값**: 계획은 전년도(2025)였으나 사용자 요청으로 **현재년도(2026)**로 변경(`financialYear` init + `getDefaultYear()` 둘 다). 표시값=조회값 일치(R5)는 유지.
- **Alpine select 초기 바인딩 desync 수정(계획 외 발견)**: `<select x-model>` + `<template x-for>` 조합에서 옵션 렌더 전 x-model 평가로 표시값이 첫 옵션으로 고정되는 race 발견. 연도 셀렉트에 `x-init="$nextTick(() => { $el.value = portfolio.financialYear })"` 추가로 해소. 학습 문서: `docs/solutions/ui-bugs/alpine-select-xfor-xmodel-init-desync-2026-05-31.md`.
- **ce:review(autofix) 보강**: race 가드(runFinancialQuery generation hoist, $nextTick 차트 stale 방지, 옵션 재시도 실패 에러 분기), SEC 에러블록 `isSecMenu()` 가드.
