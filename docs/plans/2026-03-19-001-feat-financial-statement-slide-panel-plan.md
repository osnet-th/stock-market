---
title: "feat: 포트폴리오 재무제표 우측 슬라이드 패널 UI 개선"
type: feat
status: completed
date: 2026-03-19
origin: docs/brainstorms/2026-03-19-financial-statement-ui-improvement-brainstorm.md
---

# feat: 포트폴리오 재무제표 우측 슬라이드 패널 UI 개선

## Overview

포트폴리오 화면에서 재무제표 정보를 **우측 슬라이드 패널**로 분리하여, 금액을 억원 단위로 표시하고 당기/전기/전전기 추세를 바 차트로 시각화한다. 기존 카드 내부 펼침식 패널의 공간 제약 문제를 해결하고, 재무 정보를 한눈에 파악할 수 있도록 개선한다.

## Problem Statement

1. **금액 가독성 부족**: 전체 금액이 단위 없이 표시 (`1,234,567,890`) → 자릿수를 직접 세어야 함
2. **추세 파악 어려움**: 당기/전기/전전기가 테이블 컬럼으로만 비교 → 증감 추세가 시각적으로 안 보임
3. **공간 제약**: 카드 내부 펼침식 패널에 재무 정보가 갇혀 있어 차트/테이블 배치가 어려움

(see brainstorm: `docs/brainstorms/2026-03-19-financial-statement-ui-improvement-brainstorm.md`)

## Proposed Solution

### 핵심 변경

| 항목 | 현재 | 변경 후 |
|------|------|---------|
| 화면 구성 | 카드 내부 펼침식 패널 | 우측 슬라이드 패널 (~65% 폭) |
| 금액 표시 | `1,234,567,890` | `12.3억` (기존 `Format.compactNumber()` 활용) |
| 추세 시각화 | 없음 | Chart.js 바 차트 (당기/전기/전전기 비교) |
| 메뉴 구성 | 8가지 메뉴 버튼 | 8가지 메뉴 탭 (동일 유지) |

### UI 구조

```
┌─────────────────────────────────────────────────┐
│ Portfolio (dim overlay)  │  재무 상세 패널 (~65%)  │
│                          │                         │
│  카드 목록                │  종목명 (코드)      [X]  │
│  (스크롤 가능, dim)       │  [재무계정][지표][탭...] │
│                          │  연도:[2024▼] 보고서:[연간▼]│
│                          │                         │
│                          │  ┌─ 바 차트 ────────┐  │
│                          │  │ 매출액 영업이익 순이익│  │
│                          │  │ ███   ██      █   │  │
│                          │  └──────────────────┘  │
│                          │                         │
│                          │  ┌─ 테이블 ────────┐  │
│                          │  │ 계정명   당기  전기 │  │
│                          │  │ 매출액  12.3억 10억 │  │
│                          │  └──────────────────┘  │
└─────────────────────────────────────────────────┘
```

## Technical Considerations

### 활용 가능한 기존 코드

- **`Format.compactNumber()`** (`format.js`): 이미 억/만/조 변환 로직이 구현되어 있음. 현재 재무 테이블에서 미사용 → `formatFinancialCell()`에서 활용하도록 변경
- **Chart.js**: 이미 CDN으로 로드 중 (`index.html:7`). 도넛 차트 패턴(`app.js:746-826`) 참고하여 바 차트 구현
- **Alpine.js 상태**: `portfolio.selectedStockItem` 등 기존 상태 재사용, 슬라이드 패널 open/close 상태만 추가
- **기존 API**: 8가지 재무 API 엔드포인트 변경 없이 그대로 사용

### 변경 범위

- **백엔드 변경 없음** - 순수 프론트엔드 작업
- **신규 파일 없음** - 기존 `index.html`, `app.js`, `format.js` 수정만으로 구현

## Acceptance Criteria

- [ ] 자산 카드의 "재무상세" 버튼 클릭 시 우측에서 슬라이드 패널이 열림
- [ ] 패널 폭은 화면의 약 65%, 배경은 dim 오버레이 처리
- [ ] X 버튼, 오버레이 클릭, ESC 키로 패널 닫기
- [ ] 테이블 금액 셀과 요약 카드 값 모두 `Format.compactNumber()`로 억/만 단위 표시
- [ ] `Format.compactNumber()` 억 단위에서 소수점 1자리 표시 (`12.3억`)
- [ ] 재무계정(accounts) 탭 선택 시 상단에 주요 계정 바 차트 표시
- [ ] 바 차트는 당기/전기/전전기 3개 바를 그룹으로 비교
- [ ] 기존 8가지 메뉴 탭 모두 정상 동작
- [ ] 기존 카드 내부 재무 상세 패널 제거

---

## MVP

### 작업 리스트

- [x] **작업 1**: 슬라이드 패널 HTML 구조 추가 + 기존 카드 내부 재무 상세 패널 HTML을 패널 내부로 이동 (`index.html`)
- [x] **작업 2**: `openStockDetail()` / `closeStockDetail()` 메서드 수정 + ESC 키 핸들링 추가 (`app.js`)
- [x] **작업 3**: `Format.compactNumber()` 억 단위 소수점 1자리로 변경 (`toFixed(0)` → `toFixed(1)`) (`format.js`)
- [x] **작업 4**: `formatFinancialCell()`에서 `type === 'amount'` 시 `Format.compactNumber()` 사용 + 요약 카드 `card.value`도 `Format.compactNumber()` 적용 (`app.js`)
- [x] **작업 5**: 재무계정(accounts) 탭에 Chart.js 바 차트 추가 - `financialSummaryConfig.accounts` 항목 기반 (`app.js`, `index.html`)
- [x] **작업 6**: 기존 카드 내부 재무 상세 `<template x-if>` 블록 제거 (`index.html`)

### 코드 예시

#### 작업 1: 슬라이드 패널 HTML 구조 + 기존 패널 이동

> 참고: [slide-panel-example.md](examples/slide-panel-example.md)

#### 작업 2: openStockDetail/closeStockDetail + ESC 키

> 참고: [stock-detail-methods-example.md](examples/stock-detail-methods-example.md)

#### 작업 3: Format.compactNumber 소수점 개선

> 참고: [format-compact-number-example.md](examples/format-compact-number-example.md)

#### 작업 4: formatFinancialCell + 요약 카드 포맷 변경

> 참고: [format-cell-example.md](examples/format-cell-example.md)

#### 작업 5: 바 차트 구현

> 참고: [bar-chart-example.md](examples/bar-chart-example.md)

---

## Sources

- **Origin brainstorm:** [docs/brainstorms/2026-03-19-financial-statement-ui-improvement-brainstorm.md](../brainstorms/2026-03-19-financial-statement-ui-improvement-brainstorm.md)
  - Key decisions: 우측 슬라이드 패널, 억원 단위, 바 차트, 8가지 메뉴 유지
- 기존 재무 상세 패널: `index.html:734-913`
- Alpine.js 재무 상태: `app.js:114-134`
- financialColumns: `app.js:136-189`
- 재무 메서드: `app.js:1487-1615`
- Format.compactNumber: `format.js:30-45`
- 도넛 차트 패턴: `app.js:746-826`
- 재무 API: `api.js:203-247`