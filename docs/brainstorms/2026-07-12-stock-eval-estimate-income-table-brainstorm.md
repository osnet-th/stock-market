# 종목평가 추정실적 "추정 손익"(output2) 표 가독성 개선 - Brainstorm

gate: docs/gates/2026-07-12-stock-eval-estimate-income-table-gates.md

**Date:** 2026-07-12
**Status:** Decided
**Issue:** #82 / Branch: `feat/issue-82-estimate-income-table`

## What We're Building

종목평가 화면의 **추정실적 탭 > "추정 손익" 섹션**(KIS `estimate-perform` API의 output2) 표가 한눈에 안 들어와 가독성을 개선한다. **화면 표시 방식만** 바꾼다(백엔드 로직·API·Entity·데이터 구조 변경 없음).

## 현재 상태 (실측)

- 마크업: `src/main/resources/static/partials/stock-eval.html` — 모든 섹션을 동일한 generic 표 렌더러로 그림(`sec.table.columns` / `sec.table.rows` 그대로 출력).
- 포맷: `src/main/resources/static/js/components/stock-eval.js` `fmtEstimateCell`/`fmtAmountByUnit`/`fmtEstimateLabel`.
- 백엔드: `StockEvaluationService.metricTable`가 output2를 **6행 표**로 구성 — 매출액/매출액 증감률/영업이익/영업이익 증감률/당기순이익/당기순이익 증감률 × 기간(2023.12~2027.12E). 증감률은 백엔드에서 ÷10 정규화.

## 문제점 (왜 불편한가)

1. 금액과 증감률이 **별도 6행** → 시선이 위아래로 이동.
2. 모든 셀 **좌측 정렬** → 큰 숫자(2,589,355) 자릿수 비교 불가.
3. 증감률에 **색·부호 강조 없음** → 증가/감소 구분이 한눈에 안 됨.
4. 금액 단위 기본 **'억'** → 7자리로 표시돼 읽기 부담.
5. 실적/추정(E) **구분 없음**.

## 확정된 결정 (사용자 확인 완료)

| 항목 | 결정 |
|------|------|
| 방향 | 개선안 그대로 진행 |
| 구조 | 6행 → **3줄 압축**(매출액·영업이익·당기순이익), 증감률을 각 값 **바로 아래 작은 글씨 + 색**으로 인라인 |
| 단위 | 억/조 토글 유지. (초기 '조 기본' → work 중 공유 상태 확인 후 **'억 기본 유지'**로 재결정. plan 참조) |
| 정렬 | 숫자 **오른쪽 정렬**(tabular-nums) |
| 증감률 색 | **증가=빨강(▲) / 감소=파랑(▼)** (국내 주식 관례, format.js 관례와 일치) |
| 추정치 | **E 열 음영** 처리로 실적과 구분 |
| 범위 | **추정 손익(output2)만** — 기본정보·추정 투자지표(output3)는 이번 범위 밖 |
| 절차 | **documented workflow** (GitHub Issue #82 등록 후 worktree 생성하여 진행) |

## 구현 방향

- 백엔드가 주는 output2 6행 데이터는 **그대로 두고**, 프론트에서 "추정 손익" 섹션만 감지해 3줄+증감률 인라인 표로 **재구성**.
- 나머지 섹션(기본정보·추정 투자지표)은 기존 generic 렌더러 유지.

## Edge Cases

- output2가 없거나 행 수가 예상과 다를 때 → 기존 generic 표로 폴백.
- 단위 토글('억'/'조') 전환 시 금액·라벨 단위 함께 반영.
- 증감률 값이 비어있거나 음수/0일 때 색·부호 처리.
- 다크모드/기존 색 관례 대응.

## 범위 밖 (하지 않음)

- 백엔드 API/로직/DTO 변경.
- output3(추정 투자지표) 라벨·스케일 수정(별건 이슈 검토).
- 기본정보 섹션, 재무·신용·일정 탭 변경.
