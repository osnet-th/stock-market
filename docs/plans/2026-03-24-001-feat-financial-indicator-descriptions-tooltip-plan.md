---
title: "feat: 재무제표 항목별 설명 툴팁 추가"
type: feat
status: active
date: 2026-03-24
origin: docs/brainstorms/2026-03-24-financial-indicator-descriptions-brainstorm.md
---

# feat: 재무제표 항목별 설명 툴팁 추가

## Overview

포트폴리오 재무상세 패널의 전체 메뉴(8개 탭)에서 각 항목명 옆에 ℹ️ 호버 툴팁으로 초보자 눈높이의 설명을 표시한다. 경제지표 대시보드에 이미 검증된 CSS 전용 툴팁 패턴을 재사용한다.

## Problem Statement / Motivation

재무제표의 각 지표(PER, ROE, 매출액 등)가 무엇을 의미하는지 항목명만으로는 파악이 어려워, 투자 초보자가 재무 데이터를 해석하기 힘든 상황이다.

## Proposed Solution

- **app.js**에 메뉴별로 중첩된 메타데이터 객체(`financialDescriptions`)를 정의
- 테이블의 첫 번째 text 컬럼 셀에 ℹ️ 아이콘 + 호버 툴팁 HTML을 조건부 렌더링
- 설명이 없는 항목은 아이콘을 숨김 (`x-if` 조건부 렌더링)
- 기존 ECOS 툴팁 패턴(`group/tip`, `bottom-full`, `w-72`, `bg-gray-800`)과 동일한 스타일

## Technical Considerations

### 메뉴별 이름 컬럼 매핑

| 메뉴 key | 이름 컬럼 key | 예시 값 |
|---|---|---|
| `accounts` | `accountName` | 매출액, 영업이익 |
| `indices` | `indexName` | PER, ROE |
| `full-statements` | `accountName` | 매출액, 유동자산 |
| `stock-quantities` | `category` | 보통주, 우선주 |
| `dividends` | `category` | 주당 현금배당금 |
| `lawsuits` | — (설명 불필요) | — |
| `private-fund` | `category` | — |
| `public-fund` | `category` | — |

### 메타데이터 키 구조

메뉴별로 중첩하여 동일 항목명이 다른 메뉴에서 다른 설명을 가질 수 있도록 한다:

```javascript
financialDescriptions: {
    accounts: {
        '매출액': '기업이 제품이나 서비스를 판매하여 벌어들인 총 수입입니다.',
        '영업이익': '매출에서 원가와 판매·관리비를 뺀 금액으로, 본업에서 얼마나 벌었는지 보여줍니다.',
        // ...
    },
    indices: {
        'PER': '주가를 주당순이익으로 나눈 값으로, 투자금 회수에 걸리는 연수를 뜻합니다. 낮을수록 저평가.',
        'ROE': '기업이 자기 자본으로 얼마나 이익을 내는지 보여주는 지표입니다. 높을수록 효율적.',
        // ...
    },
    // ...
}
```

### 툴팁 표시 로직

각 컬럼 정의에 `tooltip: true` 속성을 추가하여, 해당 컬럼 셀에서만 툴팁을 렌더링:

```javascript
// financialColumns 수정 예시
accounts: [
    { key: 'accountName', label: '계정명', type: 'text', tooltip: true },
    // ...
],
indices: [
    { key: 'indexName', label: '지표명', type: 'text', tooltip: true },
    // ...
]
```

### 설명 조회 헬퍼 메서드

```javascript
getFinancialDescription(menuKey, itemName) {
    const menuDescriptions = this.financialDescriptions[menuKey];
    return menuDescriptions ? menuDescriptions[itemName] : null;
}
```

### 참고할 기존 코드

- **ECOS 툴팁 HTML**: `index.html:812-822` (Variant B — `template x-if` + `group/tip`)
- **ECOS 메타데이터**: `app.js` 내 경제지표 description 필드
- **재무 테이블**: `index.html:2085-2125`, `app.js:139-192`

## Acceptance Criteria

- [ ] 8개 재무 메뉴 중 lawsuits를 제외한 7개 메뉴에 ℹ️ 툴팁이 표시된다
- [ ] 설명이 정의된 항목에만 ℹ️ 아이콘이 나타난다
- [ ] 마우스 호버 시 초보자 눈높이의 설명이 툴팁으로 표시된다
- [ ] 툴팁 스타일이 경제지표 대시보드와 동일하다 (bg-gray-800, w-72, bottom-full)
- [ ] 백엔드 API 변경 없음
- [ ] 기존 테이블 레이아웃과 기능에 영향 없음

## 작업 리스트

### 1단계: 메타데이터 정의 (`app.js`)

- [x] `financialDescriptions` 객체 작성 — 메뉴별 중첩 구조
  - `accounts`: 매출액, 영업이익, 당기순이익, 자산총계, 부채총계, 자본총계 등
  - `indices`: PER, PBR, ROE, ROA, EPS 등 수익성/안정성/성장성/활동성 지표
  - `full-statements`: 손익계산서/재무상태표 주요 계정
  - `stock-quantities`: 보통주, 우선주 등
  - `dividends`: 주당배당금, 배당수익률, 배당성향 등
  - `private-fund`, `public-fund`: 주요 구분 항목
- [x] `getFinancialDescription(menuKey, itemName)` 헬퍼 메서드 추가

### 2단계: 컬럼 설정 수정 (`app.js`)

- [x] `financialColumns`의 각 메뉴에서 이름 컬럼에 `tooltip: true` 속성 추가
  - accounts: `accountName`
  - indices: `indexName`
  - full-statements: `accountName`
  - stock-quantities: `category`
  - dividends: `category`
  - private-fund: `category`
  - public-fund: `category`

### 3단계: 테이블 HTML 수정 (`index.html`)

- [x] `index.html:2109-2113`의 `<td>` 렌더링을 수정하여 `col.tooltip === true`일 때 ℹ️ 아이콘 + 호버 툴팁 HTML을 추가
- [x] ECOS 툴팁 패턴(`index.html:812-822`)을 그대로 재사용
- [x] 설명이 없는 항목은 `x-if` 조건으로 아이콘 숨김

### 코드 예시

#### `index.html` 테이블 셀 수정 (2109-2113라인)

```html
<!-- 기존 -->
<td class="py-2 px-2"
    :class="isAmountColumn(col.type) ? 'text-right font-mono text-gray-700' : 'text-gray-700'"
    x-text="formatFinancialCell(row[col.key], col.type)"></td>

<!-- 변경 -->
<td class="py-2 px-2"
    :class="isAmountColumn(col.type) ? 'text-right font-mono text-gray-700' : 'text-gray-700'">
    <template x-if="!col.tooltip">
        <span x-text="formatFinancialCell(row[col.key], col.type)"></span>
    </template>
    <template x-if="col.tooltip">
        <span class="inline-flex items-center gap-1">
            <span x-text="formatFinancialCell(row[col.key], col.type)"></span>
            <template x-if="getFinancialDescription(portfolio.selectedFinancialMenu, row[col.key])">
                <div class="group/ftip relative inline-block">
                    <span class="text-gray-300 hover:text-gray-500 cursor-help text-xs">&#9432;</span>
                    <div class="invisible opacity-0 group-hover/ftip:visible group-hover/ftip:opacity-100
                                absolute z-10 bottom-full left-0 mb-2
                                px-3 py-2 text-xs text-white bg-gray-800 rounded-lg shadow-lg
                                w-72 max-w-xs transition-opacity duration-200"
                         role="tooltip">
                        <span x-text="getFinancialDescription(portfolio.selectedFinancialMenu, row[col.key])"></span>
                    </div>
                </div>
            </template>
        </span>
    </template>
</td>
```

## Dependencies & Risks

- **의존성**: 없음 (프론트엔드 JS/HTML만 수정)
- **리스크**: 설명 내용의 정확성 — 초보자 눈높이로 작성하되 오해를 유발하지 않도록 주의

## Sources & References

- **Origin brainstorm**: [docs/brainstorms/2026-03-24-financial-indicator-descriptions-brainstorm.md](docs/brainstorms/2026-03-24-financial-indicator-descriptions-brainstorm.md)
- **ECOS 툴팁 패턴**: `src/main/resources/static/index.html:812-822`
- **재무 테이블 렌더링**: `src/main/resources/static/index.html:2085-2125`
- **재무 컬럼 설정**: `src/main/resources/static/js/app.js:139-192`