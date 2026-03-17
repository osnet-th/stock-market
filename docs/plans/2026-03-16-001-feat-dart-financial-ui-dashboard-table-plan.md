---
title: "feat: DART 재무정보 요약 대시보드 및 테이블 UI 개선"
type: feat
status: active
date: 2026-03-16
origin: docs/brainstorms/2026-03-16-dart-financial-ui-improvement-brainstorm.md
---

# feat: DART 재무정보 요약 대시보드 및 테이블 UI 개선

## Overview

DART 재무정보 패널의 범용 테이블을 개선하여 핵심 지표를 빠르게 파악하고 상세 데이터도 편리하게 확인할 수 있도록 한다. 각 메뉴 상단에 요약 카드를 추가하고, 테이블에 한글 헤더, 숫자 포맷팅, 증감 색상, 고정 헤더 등을 적용한다.

(see brainstorm: docs/brainstorms/2026-03-16-dart-financial-ui-improvement-brainstorm.md)

## Problem Statement / Motivation

현재 재무정보는 API 응답의 영문 필드명을 그대로 컬럼 헤더로 사용하는 범용 테이블 하나로 표시된다. `currentTermAmount`, `accountName` 같은 영문 헤더가 노출되고, 숫자 포맷팅이나 증감 색상이 없어 기업 분석 시 핵심 정보를 파악하기 어렵다.

## Proposed Solution

### 1. 메뉴별 한글 헤더 매핑

`Object.keys()` 기반 동적 헤더 대신, **메뉴별 컬럼 정의 객체**를 만들어 한글 헤더 + 표시 여부 + 포맷 타입을 관리한다.

```javascript
financialColumns: {
    accounts: [
        { key: 'accountName', label: '계정명', type: 'text' },
        { key: 'currentTermAmount', label: '당기', type: 'amount' },
        { key: 'previousTermAmount', label: '전기', type: 'amount' },
        { key: 'beforePreviousTermAmount', label: '전전기', type: 'amount' }
    ],
    indices: [
        { key: 'indexName', label: '지표명', type: 'text' },
        { key: 'indexValue', label: '값', type: 'number' }
    ],
    // ... 메뉴별 정의
}
```

불필요한 컬럼(stockCode, fsDiv, statementDiv 등)은 제외하여 핵심 정보만 표시.

### 2. 요약 카드 (메뉴 상단)

각 메뉴별 핵심 지표를 `financialResult`에서 추출하여 카드로 표시:

```
┌──────────┬──────────┬──────────┬──────────┐
│ 매출액    │ 영업이익  │ 당기순이익│ 자산총계  │
│ 1조 2천억 │ 2,800억  │ 1,500억  │ 15조     │
│ ▲ +5.2%  │ ▼ -3.1%  │ ▲ +8.7%  │ ▲ +2.1%  │
└──────────┴──────────┴──────────┴──────────┘
```

#### 메뉴별 요약 카드 항목

| 메뉴 | 추출 기준 | 카드 항목 |
|------|----------|----------|
| 재무계정 | `accountName` 매칭 | 매출액, 영업이익, 당기순이익, 자산총계, 부채총계, 자본총계 |
| 재무지표 | `indexName` 매칭 | 해당 분류의 상위 3~4개 지표 |
| 배당정보 | `category` 매칭 | 주당배당금, 배당수익률, 배당성향 |
| 전체재무제표 | 없음 | 요약 카드 생략 (데이터 양이 많고 구조화 어려움) |
| 주식수량 | 없음 | 요약 카드 생략 |
| 소송/자금 | 없음 | 요약 카드 생략 (건수만 표시) |

### 3. 테이블 개선

| 개선 항목 | 적용 |
|----------|------|
| 한글 헤더 | 메뉴별 컬럼 정의로 한글 표시 |
| 불필요 컬럼 숨김 | stockCode, fsDiv, statementDiv 등 제외 |
| 숫자 포맷팅 | `Format.number()` 적용 (천단위 구분) |
| 금액 축약 | `Format.compactNumber()` (조/억/만) - 요약 카드에서 사용 |
| 증감 색상 | 전기 대비: 증가 빨강, 감소 파랑 |
| 고정 헤더 | `sticky top-0` |
| 높이 제한 | `max-h-96 overflow-y-auto` |
| 행 호버 | `hover:bg-gray-50` |
| 우측 정렬 | 숫자 컬럼 `text-right font-mono` |

## Acceptance Criteria

### 컬럼 정의 및 한글 헤더
- [x] `financialColumns` 객체 정의 (8개 메뉴별 컬럼 매핑) (`app.js`)
- [x] 테이블 헤더를 `financialColumns` 기반으로 렌더링 (`index.html`)
- [x] 불필요 컬럼(stockCode, fsDiv 등) 제외

### 요약 카드
- [x] `getFinancialSummaryCards()` 함수 구현 (`app.js`)
- [x] 재무계정: 매출액, 영업이익, 당기순이익, 자산/부채/자본총계 카드
- [x] 배당정보: 주당배당금, 배당수익률, 배당성향 카드
- [x] 전년 대비 증감 색상 (빨강/파랑) 적용
- [x] 요약 카드 HTML 영역 추가 (`index.html`)

### 테이블 개선
- [x] 숫자 컬럼 포맷팅 (`Format.number()`) 적용
- [x] 숫자 컬럼 우측 정렬 + `font-mono`
- [x] 테이블 높이 제한 (`max-h-96 overflow-y-auto`)
- [x] 고정 헤더 (`sticky top-0 bg-gray-50`)
- [x] 행 호버 (`hover:bg-gray-50`)

## Sources & References

- **Origin brainstorm:** [docs/brainstorms/2026-03-16-dart-financial-ui-improvement-brainstorm.md](docs/brainstorms/2026-03-16-dart-financial-ui-improvement-brainstorm.md) — 요약 대시보드 + 테이블 개선 접근, 차트 제외
- 재무정보 HTML: `src/main/resources/static/index.html:632-775`
- 재무정보 함수: `src/main/resources/static/js/app.js:1289-1383`
- API 호출: `src/main/resources/static/js/api.js:200-243`
- FinancialAccountResponse DTO: `src/.../stock/application/dto/FinancialAccountResponse.java` (12개 필드)
- FinancialIndexResponse DTO: `src/.../stock/application/dto/FinancialIndexResponse.java` (6개 필드)
- DividendInfoResponse DTO: `src/.../stock/application/dto/DividendInfoResponse.java` (5개 필드)
- Format 유틸: `src/main/resources/static/js/utils/format.js` (number, compactNumber)
