---
title: "feat: 포트폴리오 도넛 차트 및 섹션별 소계 표시"
type: feat
status: active
date: 2026-03-15
origin: docs/brainstorms/2026-03-15-portfolio-ui-enhancement-brainstorm.md
---

# feat: 포트폴리오 도넛 차트 및 섹션별 소계 표시

## Overview

포트폴리오 페이지 상단 영역을 Chart.js 도넛 차트 + 요약 카드 나란히 배치로 재구성하고, 각 자산 유형별 섹션 헤더에 투자금액/평가금액/수익률 소계를 표시한다. 프론트엔드만 변경하며 백엔드 API 변경은 없다.

(see brainstorm: docs/brainstorms/2026-03-15-portfolio-ui-enhancement-brainstorm.md)

## Problem Statement / Motivation

현재 상단 영역은 3개 요약 카드 + CSS 가로 막대 차트로 구성되어 있어 자산 비중을 한눈에 파악하기 어렵다. 또한 자산 유형별 투자 성과(투자금액, 평가금액, 수익률)를 확인하려면 개별 항목을 일일이 계산해야 한다.

## Proposed Solution

### 1. 상단 영역 재구성

기존 3개 요약 카드 + CSS 막대 차트 → 도넛 차트(왼쪽) + 요약 카드(오른쪽) 나란히 배치

```
┌──────────────────┬────────────────────────┐
│                  │ 총 투자 금액           │
│  [도넛 차트]     │ 120,000,000원          │
│  중앙: 총평가    │                        │
│                  │ 총 평가 금액           │
│  ● 주식 45%     │ 150,000,000원          │
│  ● 부동산 30%   │                        │
│  ● 금 15%       │ 총 수익금              │
│  ● 현금 10%     │ +30,000,000원 (+25.0%) │
└──────────────────┴────────────────────────┘
```

### 2. 자산 섹션 헤더 강화

각 자산 유형(현금 제외) 섹션 헤더에 소계 행 추가:

```
▼ 주식 (5건)
  투자: 50,000,000원 | 평가: 58,000,000원 | +16.00%
  ├ 삼성전자  ...
  └ 네이버    ...

▼ 현금성 자산 (1건)        ← 소계 없음
  ├ 비상금 통장  ...
```

## Technical Considerations

### Chart.js 인스턴스 생명주기

- `portfolio.chartInstance`에 Chart.js 인스턴스 저장
- `loadPortfolio()` 완료 후 `renderDonutChart()` 호출
- `renderDonutChart()` 내부에서 기존 인스턴스가 있으면 `destroy()` 후 재생성
- 탭 전환 시 `navigateTo('portfolio')` → `loadPortfolio()` → 차트 재생성

### 도넛 차트 중앙 텍스트

- CSS overlay 방식: canvas를 `position: relative` 컨테이너에 넣고, `position: absolute` 텍스트를 중앙 배치
- Chart.js 커스텀 플러그인 불필요, 외부 플러그인 의존성 없음

### assetTypeConfig 확장

기존 `barColor` (Tailwind 클래스)에 `chartColor` (hex) 추가:

```javascript
assetTypeConfig: {
    STOCK:       { label: '주식',     color: 'blue',   barColor: 'bg-blue-500',   chartColor: '#3B82F6' },
    BOND:        { label: '채권',     color: 'green',  barColor: 'bg-green-500',  chartColor: '#22C55E' },
    REAL_ESTATE: { label: '부동산',   color: 'yellow', barColor: 'bg-yellow-500', chartColor: '#EAB308' },
    FUND:        { label: '펀드',     color: 'purple', barColor: 'bg-purple-500', chartColor: '#A855F7' },
    CRYPTO:      { label: '암호화폐', color: 'orange', barColor: 'bg-orange-500', chartColor: '#F97316' },
    GOLD:        { label: '금',       color: 'amber',  barColor: 'bg-amber-500',  chartColor: '#F59E0B' },
    COMMODITY:   { label: '원자재',   color: 'red',    barColor: 'bg-red-500',    chartColor: '#EF4444' },
    CASH:        { label: '현금',     color: 'gray',   barColor: 'bg-gray-500',   chartColor: '#6B7280' },
    OTHER:       { label: '기타',     color: 'slate',  barColor: 'bg-slate-500',  chartColor: '#64748B' }
}
```

### 새 계산 메서드

```javascript
// 자산 유형별 투자금액 소계
getSubTotalInvested(assetType)

// 자산 유형별 평가금액 소계
getSubTotalEvalAmount(assetType)

// 자산 유형별 수익률 = (평가소계 - 투자소계) / 투자소계 * 100
getSubTotalProfitRate(assetType)
```

### 엣지 케이스 처리

| 케이스 | 처리 |
|--------|------|
| 자산 0건 | 기존 빈 상태 UI 표시, 차트 렌더링 안 함 |
| 총 평가금액 0원 | 빈 도넛 표시 방지, 차트 숨김 |
| 자산 1종류만 | 100% 단일 색상 도넛 표시 (일관성 유지) |
| 비주식 수익률 | 0.00% 표시 (investedAmount = evalAmount) |
| 주가 조회 실패 | 기존 fallback (investedAmount 사용) |

### 반응형 레이아웃

- **데스크톱 (md 이상)**: 도넛 차트 + 요약 카드 가로 배치
- **모바일 (md 미만)**: 도넛 차트 → 요약 카드 세로 배치
- Tailwind: `flex flex-col md:flex-row`

## Acceptance Criteria

- [x] Chart.js CDN 추가 (`index.html`)
- [x] `assetTypeConfig`에 `chartColor` 필드 추가 (`app.js`)
- [x] 도넛 차트 렌더링 메서드 `renderDonutChart()` 구현 (`app.js`)
- [x] 상단 영역 HTML을 도넛 차트 + 요약 카드 나란히 배치로 변경 (`index.html`)
- [x] 기존 CSS 막대 차트 영역 제거 (`index.html`)
- [x] 소계 계산 메서드 구현: `getSubTotalInvested()`, `getSubTotalEvalAmount()`, `getSubTotalProfitRate()` (`app.js`)
- [x] 자산 섹션 헤더에 소계 행 추가 (현금 제외) (`index.html`)
- [x] `loadPortfolio()` 완료 후 차트 갱신 연동 (`app.js`)
- [x] 반응형 레이아웃 (모바일/데스크톱) 확인
- [x] 엣지 케이스 처리 (0건, 0원, 1종류, 주가 실패)

## Sources & References

- **Origin brainstorm:** [docs/brainstorms/2026-03-15-portfolio-ui-enhancement-brainstorm.md](docs/brainstorms/2026-03-15-portfolio-ui-enhancement-brainstorm.md) — 도넛 차트 선택, Chart.js CDN, 상단 카드 나란히 배치, 현금 차트 포함/소계 제외
- 포트폴리오 JS: `src/main/resources/static/js/app.js` (126-136행 assetTypeConfig, 510-559행 계산 메서드)
- 포트폴리오 HTML: `src/main/resources/static/index.html` (484-531행 상단 영역)
- 기존 설계: `.claude/designs/frontend/portfolio/portfolio.md`
- 수익률 설계: `.claude/designs/frontend/portfolio-profit-calc/portfolio-profit-calc.md`