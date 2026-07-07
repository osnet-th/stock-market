---
title: "feat: 전체 재무제표 세부 계정 계층 트리 (연도별 추세)"
type: feat
status: active
date: 2026-07-06
origin: docs/brainstorms/2026-07-06-financial-statement-account-tree-brainstorm.md
---

# feat: 전체 재무제표 세부 계정 계층 트리 (연도별 추세)

## Overview

"연도별 추세" 화면 하단의 **전체 재무제표 세부 계정** 표를 평면 나열에서 **2단계 계층 트리**로 바꾼다.
DART가 명시적 계층을 주지 않으므로, `account_id`(IFRS 표준코드) 앵커 + **순서 기반 그룹핑**으로 트리를 복원한다.
각 항목 셀에는 기존 연도별 값에 더해 **전년 대비 증감(▲빨강/▼파랑, %)**을 같은 셀에 병기한다.
계층 생성은 **백엔드**에서 수행하고, 응답 DTO에 노드 구조를 실어 프론트는 렌더만 한다.
(brainstorm: docs/brainstorms/2026-07-06-financial-statement-account-tree-brainstorm.md)

## Problem Statement / Motivation

현재 세부 계정 표는 한 재무제표 종류(BS/IS/…) 안의 수십 개 계정을 계층 없이 한 줄씩 나열한다.
DART 실제 보고서는 `유동자산 > 현금/매출채권/재고…`처럼 카테고리 하위에 항목이 묶이는데, 그 구조가 사라져
"카테고리 안에서 뭐가 늘고 줄었는지"를 읽기 어렵다. 계층 복원 + 전년비 증감으로 가독성을 높인다.

## 확정된 결정 (brainstorm 승인 완료)

| 항목 | 결정 |
|------|------|
| 트리 범위 | 모든 재무제표 종류. BS/CF는 실제 트리, IS/CIS/SCE는 평면(회귀 없음, 소계 강조는 선택) |
| 계층 생성 위치 | 백엔드 (응답 DTO에 노드 구조 반영) |
| YoY 증감 | 같은 셀 병기, partial 연도는 생략 |
| 총계 표시 | 자식 없는 앵커 = 비접이식 강조 행 |

## Proposed Solution

### 핵심 아이디어: emergent role (역할 창발) 그룹핑

정적으로 "총계 vs 카테고리"를 태깅하지 않고, **앵커가 뒤따르는 항목을 자식으로 흡수하는지**로 역할을 창발시킨다.
이렇게 하면 `자산총계`(바로 다음이 `유동자산` 앵커라 자식 0개 → 총계)와 `자본총계`(뒤에 자본금·잉여금이 붙음 → 카테고리)를
한 규칙으로 자연스럽게 구분한다. 앵커 레지스트리는 단순 `Set<account_id>`면 충분(역할 사전분류 불필요).

**노드 역할(파생):**
- `TOTAL` — 자식 없는 앵커 노드. 강조 행, 토글 없음. (예: 자산총계, 부채총계, 자본과부채총계)
- `CATEGORY` — 자식 있는 앵커 노드. 접이식 부모. (예: 유동자산, 비유동부채, 자본총계, 기타(미분류))
- `ITEM` — 앵커에 속하지 않은 평면 행. (평면 statement의 모든 행)

**statement별 모드:**
- **컨테이너 모드 (BS, CF)**: 앵커 레지스트리 보유 → 순서 기반 그룹핑.
- **평면 모드 (IS, CIS, SCE)**: 앵커 레지스트리 없음 → 트리 미형성. 단 IS/CIS는 **emphasis-set**(손익 마일스톤 account_id)에 해당하는 행을 `TOTAL`(강조)로, 나머지는 `ITEM`으로 렌더. SCE는 emphasis 미적용(전 행 `ITEM`).

### 그룹핑 알고리즘 (컨테이너 모드)

병합된 평면 행 리스트(이미 최신 연도의 DART 원본 순서 유지됨)를 위→아래로 1-pass:

```
open = null            // 현재 열린 앵커 노드 {row, children:[]}
orphans = []           // 첫 앵커 이전에 나온 평면 행
for row in rows:
    if isAnchor(statementDiv, row.id):
        flush(open) -> nodes          // open의 children 수에 따라 TOTAL/CATEGORY 확정
        open = { row, children: [] }
    else if open != null:
        open.children.add(row)        // 순서상 직전 앵커의 자식
    else:
        orphans.add(row)              // 선두 orphan
flush(open) -> nodes
if orphans not empty:
    nodes.add( category("기타(미분류)", orphans) )   // 앵커가 하나라도 있었으므로 미분류 그룹으로
```

- `open`은 첫 앵커 이후엔 항상 non-null로 재할당되므로 `orphans`는 **선두 구간만** 모인다(결정적).
- 앵커 매칭 키 = `TimelineRow.id`. 현재 `detailRowKey`가 account_id(표준) 또는 계정명(미사용)을 담으므로, 표준 IFRS 앵커는 id로 정확히 매칭되고 미사용 계정은 앵커에 걸리지 않음(의도대로).
- **미분류 항목 이름은 원본 유지** — "기타(미분류)"는 그룹 이름일 뿐 항목명은 손대지 않음.

### 앵커 레지스트리 (Phase 0 실측 확정) — 2종 앵커

Phase 0 실호출(삼성전자 005930, 2023 사업보고서, CFS)로 확정. **CATEGORY**(뒤따르는 항목을 자식으로 흡수) 외에
**TERMINAL**(단독 강조 행, 현재 카테고리를 닫되 자식을 흡수하지 않음)이 CF에 필요함이 실측으로 드러남.

```
BS  (category): ifrs-full_Assets, ifrs-full_CurrentAssets, ifrs-full_NoncurrentAssets,
                ifrs-full_Liabilities, ifrs-full_CurrentLiabilities, ifrs-full_NoncurrentLiabilities,
                ifrs-full_Equity, ifrs-full_EquityAndLiabilities
    (terminal): (없음)
CF  (category): ifrs-full_CashFlowsFromUsedInOperatingActivities,
                ifrs-full_CashFlowsFromUsedInInvestingActivities,
                ifrs-full_CashFlowsFromUsedInFinancingActivities
    (terminal): dart_CashAndCashEquivalentsAtBeginningOfPeriodCf,     # 기초현금
                dart_CashAndCashEquivalentsAtEndOfPeriodCf,           # 기말현금
                ifrs-full_EffectOfExchangeRateChangesOnCashAndCashEquivalents,  # 외화환산효과
                ifrs-full_IncreaseDecreaseInCashAndCashEquivalents    # 현금순증감
IS/CIS/SCE: (레지스트리 없음 — 평면 모드)
```

**Phase 0 실측 요지:**
- `ord` = 응답 배열 순서(이미 정렬됨) → **ord 재정렬 불필요**. 기존 병합 행 순서 유지.
- BS: 응답 순서가 논리적. emergent 알고리즘으로 자산총계(TOTAL)/유동·비유동자산(CATEGORY)/자본총계(CATEGORY, 자본 컴포넌트 흡수)/자본과부채총계(TOTAL)/유동부채(CATEGORY)/부채총계(TOTAL)/비유동부채(CATEGORY) — **검증 완료, 양호**.
- CF: DART 원본 순서가 `기초·기말현금 → (매각예정) → 재무 → 투자 → 영업 → 외화환산 → 순증감`으로 뒤섞임. TERMINAL 앵커로 기초/기말/외화/순증감을 단독 행으로 빼면 영업/투자/재무 3개 카테고리가 깔끔. 회사별 순서 편차에도 강건.
- IS: 소계 id(매출총이익=ifrs-full_GrossProfit, 영업이익=dart_OperatingIncomeLoss, 법인세차감전순이익=ifrs-full_ProfitLossBeforeTax, 당기순이익=ifrs-full_ProfitLoss 등) 확인됨. IS 응답 순서는 불규칙(계정코드 정렬 경향)이라 트리는 부적합하나, **소계 강조(emphasis-set)는 사용자 요청으로 도입**(아래 참고). 일부 종목(SK하이닉스·KB금융)은 별도 IS 없이 CIS에 손익 내용을 담으므로 emphasis는 IS·CIS 모두 적용.

### 알고리즘 (3-way 분류)

```
for row in rows:
    kind = classify(anchors, row.id)   // CATEGORY | TERMINAL | PLAIN
    CATEGORY : flush(open) -> nodes; open = { row, [] }
    TERMINAL : flush(open) -> nodes; open = null; nodes.add( totalNode(row) )
    PLAIN    : open!=null ? open.children.add(row) : orphans.add(row)
flush(open) -> nodes
if orphans not empty: nodes.add( categoryNode("기타(미분류)", orphans) )
// flush: open의 children 수로 TOTAL(0)/CATEGORY(>0) 확정
```

### Backend 변경 (신규 클래스 1 + DTO 수정 + assembler 연결)

```
stock/application/
├── FinancialDetailTreeBuilder.java     # [신규] 평면 행 → 노드 트리 (@Component)
│     - buildNodes(String statementDiv, List<TimelineRow> rows) : List<TimelineDetailNode>
│     - 앵커 레지스트리 상수(Map<String, Set<String>>) 보유
│     - 컨테이너/평면 모드 분기, 1-pass 그룹핑, 기타(미분류) 처리
├── FinancialTimelineAssembler.java     # [수정] toDetailGroup에서 mergeRows 결과를 treeBuilder로 변환
└── dto/FinancialTimelineResponse.java  # [수정] TimelineDetailGroup.accounts → nodes,
                                        #        TimelineDetailNode(role, row, children) 신규
```

- **신규 DART 호출 없음**, **Entity 없음**, **신규 엔드포인트 없음**, **패키지/레이어 변경 없음**.
- 순수 후처리(병합된 행 위 변환)만 추가. 데이터 수집(`FinancialTimelineService`)은 무변경.
- 코드 컨벤션 준수: `buildNodes`를 mode 분기(guard) → `groupedNodes`/`flatNodes` private helper로 분리, 각 5줄 내외 유지.

### DTO 스키마 변경 (Approval Gate — 응답 형태 변경)

```java
// TimelineDetailGroup: accounts(List<TimelineRow>)  →  nodes(List<TimelineDetailNode>)
public static class TimelineDetailGroup {
    private final String statementDiv;
    private final String statementName;
    private final List<TimelineDetailNode> nodes;
}
// 신규
public static class TimelineDetailNode {
    private final String role;                // "TOTAL" | "CATEGORY" | "ITEM"
    private final TimelineRow row;            // 기존 {id, name, values}
    private final List<TimelineRow> children; // CATEGORY만 non-empty, 그 외 빈 리스트
}
```

- 유일 소비자: 우리 프론트(`financial.js` + `portfolio-deposit-financial.html`). `StockEvaluationService.details`/`stock-eval.html`은 **무관**(다른 바인딩) — 확인 완료.

### Frontend 변경 (financial.js + HTML 파티션)

`financial.js`:
- `getTimelineDetailGroups()` — 그대로(이제 `group.nodes` 사용).
- `detailGroupAccountCount(group)` — 헤더의 "N개 계정" 재계산(leaf 합계).
- 카테고리 접이식 상태: `portfolio.timelineExpandedDetailCategories` 맵 추가 + `isDetailCategoryExpanded(key)` / `toggleDetailCategory(key)` (key = `statementDiv + '::' + node.row.id`).
- YoY: `timelineDetailDelta(row, colIdx)` — `columns[colIdx-1]` 대비 증감률. partial 컬럼 또는 직전 값 없음/0이면 null. `timelineDetailDeltaClass`/`timelineDetailDeltaText`로 ▲빨강/▼파랑/`-`.
- `timelineDetailCell(row, col)` — 기존 값 포맷 유지(값은 그대로, 델타는 별도 span).

`portfolio-deposit-financial.html` (세부 계정 섹션, 609~652):
- `group.accounts` 반복 → `group.nodes` 반복.
- 노드 role별 렌더:
  - `TOTAL`: `<tr class="font-semibold text-gray-800">` 값 셀 + 델타.
  - `ITEM`: 기존 `<tr>` 값 셀 + 델타.
  - `CATEGORY`: 카테고리 헤더 `<tr>`(name + child count + 토글 ±) → 펼침 시 `node.children` 각 행을 들여쓰기(`pl-6`)로 렌더 + 델타.
- 각 값 셀: `x-text` 값 + 옆에 델타 `<span :class="timelineDetailDeltaClass(...)" x-text="timelineDetailDeltaText(...)">`.

## 화면 스케치 (BS 예)

```
▾ 재무상태표 (34개 계정)
   계정                 2022      2023      2024(진행중)
   자산총계 (강조)      120조 ▲5%  126조 ▲5%   —
   ▸ 유동자산 (12)      50조 ▲3%   52조 ▲4%    —
   ▸ 비유동자산 (18)    70조 ▲6%   74조 ▲5%    —
   부채총계 (강조)      40조 ▼2%   39조 ▼3%    —
   ▸ 유동부채 (9)       ...
   ▸ 비유동부채 (6)     ...
   ▾ 자본총계 (7)       80조 ▲8%   87조 ▲9%    —
       자본금           ...
       이익잉여금        ...
   자본과부채총계 (강조) ...
```

## Implementation Phases

### Phase 0 — 앵커 id 실측 확정 (완료)
- [x] CF 활동 account_id 확인 — 영업/투자/재무 3개 확정. 추가로 CF 순서 뒤섞임 발견 → TERMINAL 앵커(기초/기말/외화/순증감) 도입 결정.
- [x] BS 8개 앵커 순서 실측 — emergent 알고리즘으로 양호한 트리 형성 확인(자본총계가 자본 컴포넌트를 흡수).
- [x] IS/CIS 소계 id 확인 — 확인했으나 IS 응답 순서 불규칙 → 평면 유지, emphasis-set 미도입.

### Phase 1 — Backend (완료)
- [x] `FinancialTimelineResponse`: `TimelineDetailNode` 추가, `TimelineDetailGroup.accounts` → `nodes`.
- [x] `FinancialDetailTreeBuilder` 신규: 앵커 레지스트리(CATEGORY/TERMINAL) + `buildNodes` (mode 분기 → grouped/flat helper) + `GroupingState` 상태객체.
- [x] `FinancialTimelineAssembler.toDetailGroup`: `mergeRows` 결과를 `detailTreeBuilder.buildNodes(...)`로 변환. `@RequiredArgsConstructor` 주입.
- [x] `./gradlew compileJava` 통과.

### Phase 2 — Frontend (완료)
- [x] `financial.js`: `getDetailRenderRows`(트리 평탄화), 카테고리 토글, `detailGroupAccountCount`, YoY 델타 헬퍼(`timelineDetailDelta/Text/Class`), role별 스타일 헬퍼.
- [x] `portfolio.js`: `timelineExpandedDetailCategories` 상태 추가.
- [x] `portfolio-deposit-financial.html`: 세부 계정 섹션을 노드 role 기반 렌더로 교체 + 값 셀에 델타 병기.
- [x] IS/CIS 소계 emphasis-set — **도입(사용자 요청)**. `INCOME_MILESTONES` 상수(매출액·매출총이익·영업이익·법인세차감전순이익·계속영업이익·당기순이익·기타포괄손익·총포괄손익)를 IS·CIS 평면 모드에서 `TOTAL`(강조)로 렌더. 프론트 무변경(기존 TOTAL 스타일 재사용). SCE 미적용.

### Phase 3 — 검증 (완료)
- [x] Phase 0 데이터로 순수 알고리즘 시뮬레이션 — BS/CF 트리, IS 평면 기대와 일치.
- [x] dev 프로파일 + 로컬 PostgreSQL 앱 기동, `/api/stocks/005930/financial/timeline?years=3&fsDiv=CFS&items=DETAILS` 실호출 — 노드 트리 JSON 정상(BS 8노드 트리, CF TERMINAL+기타(미분류), IS/CIS/SCE 평면).
- [x] 프론트 렌더(실 응답 seed harness + 브라우저 스크린샷): BS 트리·CF 트리(영업활동에 외화/순증감 미포함 확인)·IS/CIS/SCE 평면, YoY ▲빨강/▼파랑, partial 연도 델타 생략·앰버 강조, 접이식 토글 동작 확인.
- [x] 콘솔 에러 없음(Tailwind CDN 경고만, harness 한정).

## Testing Strategy

- 명시적 테스트 요청은 없으나(테스트는 요청 시), `FinancialDetailTreeBuilder`는 **순수 함수**(입력 List → 출력 List)로 설계해 필요 시 단위테스트 용이하게 유지.
- 수동 검증(Phase 3)이 1차. 대표 케이스: 표준 BS(트리), 표준코드 미사용 많은 종목(폴백), 우선주 유무, 진행중 연도 존재/부재.

## Risks / Trade-offs

- **응답 DTO 형태 변경(Approval Gate)**: `accounts`→`nodes`. 소비자가 우리 프론트뿐이라 blast radius 작음. 플랜 승인으로 게이트 해소.
- **IS/CIS/SCE는 실질적 트리 아님**: 계층이 얕아 평면 유지. "모든 종류 트리화" 기대와 갭이 있을 수 있으나 brainstorm에서 근거 설명·합의됨. 소계 강조는 선택 확장.
- **앵커 id 회사별 편차**: 표준 IFRS 코드는 상장사 공통이나 커스텀 종목은 앵커 미검출 → 평면 폴백(안전). 트리 미형성이 오류는 아님.
- **YoY "전년"의 근사**: 컬럼이 연도 스킵 시 직전 컬럼이 2년 전일 수 있음 → "직전 표시 기간 대비"로 해석. 일반 연간 타임라인에선 정확히 전년.
- **과거 전용 계정의 소속 애매**: 최신 연도 순서를 정본으로 삼음. 최신에 없던 trailing 계정은 마지막 카테고리 또는 기타로 귀속(문서화된 엣지).

## Approval Gates (이 작업에서 발생)

1. **응답 DTO 형태 변경** — `TimelineDetailGroup.accounts → nodes` + `TimelineDetailNode` 신규. → **본 플랜 승인 시 함께 승인**.
- Entity 변경 없음 / 신규 public 엔드포인트 없음 / 패키지·레이어·의존성 방향 변경 없음.

## Out of Scope

- 요약 표/차트, 재무지표 4분류 표, 공시 탭.
- 3단계 이상 트리, XBRL 원문 파싱, 회사 간 비교.
- 데이터 수집/DART 호출 로직 변경.

## 부가 작업 (같은 브랜치, 사용자 요청): 재무상세 패널 가로 리사이즈

계정 트리와 별개 UI 기능이나 사용자 요청으로 **같은 브랜치**에 포함(worktree 분리 권장은 안내함).

- 우측 슬라이드 패널 왼쪽 가장자리에 드래그 핸들 추가 → 가로 너비 조절. **최소 480px, 최대 100vw(화면 전체)**. 데스크톱(lg+) 전용, 모바일은 기존 `w-full` 유지.
- 조절 너비는 **localStorage**(`financialPanelWidth`)에 저장·복원. 핸들 **더블클릭 시 기본(65%) 복원**.
- 프론트 전용: `portfolio.js`(상태 `financialPanelWidth`), `financial.js`(`startFinancialPanelResize`/`financialPanelBodyStyle`/`financialPanelHandleStyle`/`_applyPanelWidth`/`_savePanelWidth`/`resetFinancialPanelWidth`), `portfolio-deposit-financial.html`(핸들 div + 패널 `:style` 바인딩). 백엔드 무변경.
- 검증: 격리 harness(1440px)로 기본 65%·드래그 리사이즈·최대(100vw)/최소(480px) 클램프·localStorage 저장/복원/리셋·데스크톱 한정·콘솔 에러 없음 확인.
