---
title: 포트폴리오 그래프 가독성 개선
type: feat
status: active
date: 2026-08-02
issue: https://github.com/osnet-th/stock-market/issues/107
origin: docs/brainstorms/2026-08-02-portfolio-graph-readability-brainstorm.md
gate: docs/gates/2026-08-02-portfolio-graph-readability-gates.md
---

# 포트폴리오 그래프 가독성 개선 (#107)

## Overview

포트폴리오 화면 표시 문제 3건을 수정한다: ① 도넛 캔버스 내장 범례 제거·중앙 텍스트 원 중심 배치(겹침 해소), ② 자산 비중 미니 막대 radius 축소(소수% 뭉개짐 해소), ③ 목표 자산 배분 카드를 0 기준 다이버징 편차 막대로 재설계(색=행동 방향, 만원 축약·행동 언어, 리밸런싱 요약 1줄). 프론트엔드 전용 — 서버 변경 없음.

## 변경 파일

| 파일 | 변경 |
|---|---|
| `static/partials/portfolio.html` | 도넛 오버레이 고정 오프셋(`bottom: 48px`) 제거·정중앙 정렬 / 자산 비중 막대 `rounded-full`→`rounded` / 목표 배분 카드 마크업 재구성(리밸런싱 요약 1줄 + 버킷·자산군 다이버징 편차 행 + 색·밴드 범례, 현재원/목표원 캡션 제거) |
| `static/js/components/portfolio.js` | `renderDonutChart` legend 비활성(generateLabels 제거) / 다이버징 편차 헬퍼 신규(`getDeviationScale`·`getDeviationBarStyle`·`getDeviationBandStyle`·`formatDeviationLabel`·`formatKrwCompact`·`getRebalanceSummary`) / 교체로 미사용이 되는 `formatAllocDeviation`·`getAllocationBucketBarStyle`·`getAllocationTargetMarkerStyle` 제거 |

공개 API·Entity·서버 로직 변경 없음. `/api/portfolio/allocation/status` 응답을 그대로 사용.

## 표시 규칙 (brainstorm 확정 반영)

- 편차 막대: 중앙 0 기준, 부족은 왼쪽·초과는 오른쪽. 스케일은 `max(밴드×2, ceil(최대 |편차%p|))`로 동적 산정.
- 허용밴드: 중앙 ±밴드%p 폭 음영. 밴드 값은 응답의 `bandPctPoint` 사용.
- 색: 밴드 초과+초과=빨강(줄이기), 밴드 초과+부족=파랑(채우기), 밴드 내=회색. 편차 0(±0.05%p 미만)은 "목표 일치".
- 라벨: `+25.0%p · 751만원 많음` / `−17.4%p · 996만원 부족` — 만원 미만 반올림, 1억 이상은 억 단위 병행(`1억 2,300만원`).
- 목표 미설정 자산군(targetRatio null): 현재 %만 + "목표 미설정", 편차 막대 없음.
- 리밸런싱 요약: 상위 버킷 밴드 초과 시 "투자자산에서 안전자산으로 약 996만원 이동 시 목표 도달"(방향은 부족 버킷 기준), 모두 밴드 내면 "안전·투자 비율이 허용밴드 안에 있습니다".
- 현재/목표 %는 각 행 보조 열로 유지(`47.6% / 65.0%`). "배분 제외: 암호화폐 N건" 문구 유지.
- 도넛: 범례 제거 후 정사각 캔버스 중심 = 원 중심이므로 오버레이는 `inset-0` 중앙 정렬만 사용.

## Implementation Steps

- [x] `renderDonutChart` — `legend.display: false`, `generateLabels` 제거
- [x] 도넛 오버레이 — 고정 `bottom: 48px` 제거, 정중앙 정렬
- [x] 자산 비중 막대 — 트랙·채움 `rounded-full` → `rounded`
- [x] 목표 배분 카드 마크업 — 요약 1줄 + 상위 버킷 2행 + 투자자산 내부 6행(다이버징) + 범례
- [x] JS 헬퍼 신규 6종 추가, 미사용 헬퍼 3종 제거
- [x] `node --check` + worktree bootRun(:8081)으로 브라우저 실측 검증

## 범위 확장 (2026-08-02, 태형님 요청)

매도 이력 탭 상단에 전체 합계 카드 추가:

- 표시 값: 총 매도 건수 / 실입금 합계(Σ `saleNetProceeds`) / 실현손익 합계(Σ `saleNetProfit`, +빨강·−파랑) / 실현 수익률(손익합 ÷ 원가합 ×100, 원가 = 실입금 − 손익 — KRW 기준이라 통화 혼합 안전)
- 구현: `portfolio.js`에 `getSalesSummary()` 헬퍼, `portfolio.html` 매도 이력 탭 리스트 위 카드 1개 (기존 월별 그룹 헤더의 실입금/손익 표기 관례 재사용)
- [x] `getSalesSummary()` 헬퍼 추가
- [x] 매도 이력 탭 상단 합계 카드 마크업
- [x] 브라우저 실측 검증

## Validation

- `node --check src/main/resources/static/js/components/portfolio.js`
- worktree에서 `SERVER_PORT=8081 ./gradlew bootRun` (dev 프로필, 로컬 postgres 공유) 후 브라우저 확인:
  - 도넛 중앙 텍스트가 구멍 중심에 정확히 위치(자산 9종 상태)
  - 2%·3% 미니 막대 형태 정상
  - 배분 카드: 다이버징 방향·밴드 음영·라벨·리밸런싱 요약 문구
  - 자산 1종만 있는 케이스·목표 미설정 케이스는 JS 하네스 또는 임시 데이터로 확인
- 스크린샷 증빙 첨부

## 리스크

- Chart.js 범례 제거 시 도넛 크기가 커져 좌우 카드 높이 균형이 달라질 수 있음 → max-width 280px 유지로 흡수
- 다이버징 스케일이 극단 편차(예: +25%p)에서 밴드 음영을 과소 표시할 수 있음 → 동적 스케일 + 최소폭 보장
