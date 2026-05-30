---
date: 2026-05-30
topic: financial-detail-explicit-query
---

# 재무상세 조회 - 명시적 조회 버튼 방식 전환

## Problem Frame

포트폴리오 화면에서 국내(KR) 종목의 재무상세를 조회할 때 두 가지 문제가 있다.

1. **표시값/조회값 불일치**: 연도 셀렉트박스 초기값은 2026(현재연도, `financialYear`)으로 보이지만, 종목 오픈 시 `getDefaultYear()`가 2025(현재연도-1)로 덮어쓰면서 실제로는 2025 데이터를 조회한다. 사용자가 보는 값과 조회되는 값이 어긋난다.
2. **자동 조회로 인한 통제 부족**: 메뉴 탭을 클릭하면 `selectFinancialMenu()`가 곧바로 `loadSelectedFinancial()`를 호출해 자동 조회한다. 사용자가 원하는 연도/보고서타입을 먼저 지정하고 조회하기 어렵다.

사용자(투자자)는 원하는 연도와 보고서타입을 명시적으로 지정한 뒤 조회 버튼을 눌러 조회하기를 원한다.

## Requirements

**조회 트리거**
- R1. 메뉴 탭 클릭은 선택 메뉴만 전환하고 자동 조회하지 않는다. 데이터 조회는 오직 [조회] 버튼으로만 발생한다.
- R2. 종목 재무상세를 처음 열 때도 자동 조회하지 않는다.
- R3. 어떤 파라미터 셀렉트박스의 값 변경만으로도 조회가 트리거되지 않는다. KR 재무 화면에는 `@change="onFinancialFilterChange()"`로 자동 조회되는 셀렉트가 4개 있다 — 연도, 보고서타입, 지표분류(`financialIndexClass`), 재무제표구분(`financialFsDiv`). 네 개 모두 자동 조회를 제거한다.
- R4. [조회] 버튼 클릭 시 현재 선택된 메뉴 + 연도 + 보고서타입으로 조회한다.

**선택 상태 / 표시값**
- R5. 연도 셀렉트박스에 표시되는 값과 실제 조회되는 연도가 항상 일치한다(2026/2025 불일치 버그 해소).
- R6. 메뉴 탭을 전환해도 사용자가 선택한 연도/보고서타입은 유지된다.
- R7. 아직 조회하지 않은 메뉴는 빈 상태(예: "조회 버튼을 눌러 데이터를 조회하세요")를 표시한다.

## Success Criteria
- 연도 셀렉트박스에 보이는 값과 조회 결과의 연도가 일치한다.
- 탭을 눌러도 자동 조회되지 않고, [조회] 버튼을 눌러야만 네트워크 요청이 발생한다.
- 원하는 연도+보고서타입을 지정한 뒤 한 번의 조회로 해당 데이터를 볼 수 있다.

## Scope Boundaries
- 해외(SEC) 주식의 분기/연간 토글 방식은 변경하지 않는다(국내 KR만 대상).
- 소송현황(lawsuits) 메뉴는 보고서타입을 쓰지 않고 기간(startDate/endDate)으로 조회한다. 단, 코드상 이 기간은 별도 날짜 피커가 아니라 동일한 연도 셀렉트값에서 파생된다(`getLawsuits(stockCode, year+'0101', year+'1231')`). 따라서 lawsuits는 "연도와 무관"한 게 아니라 보고서타입만 제외될 뿐 연도 셀렉트는 공유한다. 자동 조회 제거(R1) 원칙은 동일하게 적용하며, lawsuits의 [조회]/연도 셀렉트 노출 동선은 Outstanding Questions에서 확정한다.
- 백엔드 API 시그니처(year, reportCode 파라미터)는 변경하지 않는다. 프론트엔드 조회 트리거 방식만 변경한다.

## Key Decisions
- 조회 방식: 탭 클릭/셀렉트 변경 자동 조회를 모두 제거하고 명시적 [조회] 버튼으로만 조회. (사용자가 원하는 연도/타입을 통제하기 위함. 2026/2025 버그 수정과 자동조회 제거가 별개라는 product-lens 지적을 인지했으나, 사용자가 명시적 조회 버튼 방식을 요청·확정함)
- 적용 범위: 국내(KR)만. (해외 SEC는 UI 구조가 다르고 현재 요청 범위 밖) — SEC는 탭클릭 자동조회를 유지하므로 동일 패널 내 KR(수동)/SEC(자동) 2-모드 비대칭이 발생하나, 본 작업에서는 의도적 수용 후 별도 과제로 둔다.
- 연도 자동 폴백 제거: `shouldFallbackFinancialYear`(조회 결과가 비면 무음으로 전년도 재조회 + 셀렉트값 변경)를 제거한다. 사용자가 선택한 연도 그대로만 조회하며, 데이터가 없으면 빈 결과 안내를 표시한다. (R5 "표시 연도 == 조회 연도 항상 일치" 보장)
- 기본 선택값: 연도는 전년도(현재연도-1, 2025) 기본 선택을 유지하되 셀렉트 표시값과 일치시킨다.

## Dependencies / Assumptions
- 관련 프론트엔드: `src/main/resources/static/js/components/financial.js`, `portfolio.js`, `partials/portfolio-deposit-financial.html`
- 보고서타입 옵션은 `/api/stocks/financial/options`에서 동적 로드(기존 유지).

## Outstanding Questions

### Deferred to Planning
- [Affects R7][Technical] 미조회 상태 구분을 위한 센티넬 설계 — `financialResult`의 `null`(아직 조회 안 함)과 빈 배열 `[]`(조회했으나 0건)을 구분해야 한다. 현재 세 빈 상태(메뉴 미선택 / 미조회 / 조회 후 0건)가 충돌하므로 각 조건·문구를 분리 정의.
- [Affects R7][Technical] 메뉴 전환 시 기존 조회 결과(`financialResult`)를 즉시 초기화할지, 마지막 결과를 유지할지.
- [Affects R6][Technical] 연도/보고서타입 상태를 메뉴별로 둘지 전역 1개로 둘지(현재 전역 `financialYear`/`financialReportCode`). 추가로 `financialIndexClass`/`financialFsDiv`도 전역 상태이므로 동일 정책을 적용할지 함께 결정.
- [Affects R4][Needs research] 동적 로드되는 보고서타입(reportCode) 옵션 집합이 모든 KR 메뉴에서 동일한지. 다르다면 전역 유지된 reportCode가 새 메뉴에서 무효일 때 동작(기본값 리셋 vs 빈 결과) 정의.
- [Affects R4][Technical] [조회] 버튼 사양 — 배치(파라미터 행 끝 vs 별도 행), 레이블, 로딩 중 disabled/중복요청 방지, 옵션 미로드 시 disabled 여부.
- [Affects R4][Technical] KR 조회 실패(네트워크/서버 에러) 시 표시·재시도 동선. 기존 `secFinancialError`는 SEC 전용이라 KR 경로엔 에러 상태가 없다.
- [Affects Key Decisions][Needs research] 기본 연도 2025 및 옵션에 현재연도(2026)를 포함하는 것이 타당한지 — 연차보고서 미공시 시 폴백을 제거하면 첫 조회가 빈 결과로 끝날 수 있음.

## Next Steps
→ /ce:plan for structured implementation planning
