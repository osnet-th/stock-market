---
date: 2026-05-03
module: favorite
title: 관심지표 GRAPH 카드 차트 스타일 ECOS 동일화
---

# 관심지표 GRAPH 카드 차트 스타일 ECOS 동일화

## 배경

- 현재 관심지표 GRAPH 카드의 차트는 fill 영역 + 축 비표시의 스파크라인 스타일.
- 사용자 요청: ECOS 페이지(`국내 경제지표 → 차트 뷰`) 의 차트와 동일한 스타일로 통일.
- 카드 레이아웃(가로 스크롤, `w-[280px]`)은 그대로 유지, **차트 옵션만** ECOS 와 동일화.

## 변경 대상

`src/main/resources/static/js/components/favorite.js:196-250` 의 `renderFavoriteChart` 함수 차트 옵션.

`src/main/resources/static/index.html` 의 GRAPH 카드 차트 컨테이너 높이(`h-20`) 조정.

## ECOS vs 현재 차트 옵션 비교

| 항목 | ECOS (참조) | 현재 favorite | 변경 후 |
|---|---|---|---|
| `borderColor` | `#3B82F6` | `#3b82f6` | `#3B82F6` |
| `borderWidth` | `1.5` | `1.5` | `1.5` |
| `pointRadius` | `0` | `0` | `0` |
| `pointHoverRadius` | `4` | (없음) | `4` |
| `tension` | `0.3` | `0.3` | `0.3` |
| `fill` | `false` | `true` | `false` |
| `backgroundColor` | (없음) | `rgba(59,130,246,0.1)` | (제거) |
| `spanGaps` | `false` | `true` | `false` |
| `tooltip.mode` | `'index'` | (기본) | `'index'` |
| `tooltip.intersect` | `false` | (기본) | `false` |
| `scales.x.display` | `true` | `false` | `true` |
| `scales.x.ticks` | `maxRotation:45, autoSkip, maxTicksLimit:8, font:10` | (없음) | 동일 |
| `scales.x.grid.display` | `false` | (기본) | `false` |
| `scales.y.display` | `true` | `false` | `true` |
| `scales.y.ticks` | `maxTicksLimit:5, font:10` | (없음) | 동일 |

## 차트 컨테이너 높이

축 표시를 위해 카드 내 차트 영역 높이 상향:
- 현재: `h-20` (80px) → 너무 좁아 축 라벨이 차트 영역을 잠식
- 변경: `h-40` (160px) — 가로 스크롤 카드 폭(280px) 대비 적절한 비율, ECOS `h-48` 대비 살짝 작아 카드 컴팩트성 유지

## 툴팁 라벨 동작

- ECOS 페이지: `${val} ${indicator.unitName || ''}`
- 관심지표 ECOS 카드: DTO 에 `unitName` 없음 → 값만 출력
- 관심지표 GLOBAL 카드: DTO 에 `unit` 있음 → `${val} ${unit}` 출력 가능

`renderFavoriteChart` 호출 시 `unit` 인자(optional) 추가하여 두 케이스 모두 처리:
- 호출 시그니처: `renderFavoriteChart(canvasEl, history, indicatorCode, unit)`
- 호출부 2곳 (`index.html:436`, `index.html:573`):
  - ECOS: `renderFavoriteChart($el, card.history, card.indicatorCode)` — 기존 그대로
  - GLOBAL: `renderFavoriteChart($el, card.history, card.indicatorCode, card.unit)`

## 작업 리스트

- [x] `favorite.js:196` `renderFavoriteChart` 시그니처에 optional `unit` 파라미터 추가
- [x] `favorite.js:214-247` 차트 옵션을 ECOS 동일 스타일로 변경 (위 표 기준)
- [x] `favorite.js` 툴팁 콜백에서 `unit` 사용해 `${val} ${unit ?? ''}` 출력
- [x] `index.html:434` ECOS GRAPH 카드 차트 컨테이너 `h-20 → h-40`
- [x] `index.html:571` GLOBAL GRAPH 카드 차트 컨테이너 `h-20 → h-40`
- [x] `index.html:573` GLOBAL `renderFavoriteChart` 호출에 `card.unit` 전달
- [ ] 브라우저 새로고침 후 ECOS / GLOBAL GRAPH 카드 시각 검증

## 추가 변경 (요청 반영)

관심지표 카드 왼쪽의 파란/회색 줄(`spread-card` border-left + `status-normal`) 모두 제거.

방식: 4개 카드의 클래스 `spread-card ... :class="...status-normal..."` →
`bg-white border border-gray-200` 으로 교체 (ECOS 차트 카드와 동일).

| 라인 | 변경 |
|---|---|
| index.html:420-421 | ECOS GRAPH 카드 |
| index.html:469-470 | ECOS INDICATOR 카드 |
| index.html:553-554 | GLOBAL GRAPH 카드 |
| index.html:607-608 | GLOBAL INDICATOR 카드 |

`custom.css` 의 `.spread-card` 정의는 유지 (다른 곳에서 사용 중).

추가 작업 리스트:
- [x] index.html 4개 카드의 `spread-card` + `:class="status-normal"` 제거 → `bg-white border border-gray-200` 적용

## 비-목표

- 카드 레이아웃 변경(가로 스크롤 → 그리드) 없음
- DTO / 백엔드 변경 없음
- ECOS 페이지 자체 변경 없음
- 차트 라이브러리 / 색상 팔레트 변경 없음
- `custom.css` 의 `.spread-card` 정의 변경 없음 (다른 컴포넌트 사용 중)
