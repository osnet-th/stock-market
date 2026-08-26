---
title: "feat: 종목평가 추정실적 '추정 손익'(output2) 표 가독성 개선"
type: feat
status: active
date: 2026-07-12
issue: 82
origin: docs/brainstorms/2026-07-12-stock-eval-estimate-income-table-brainstorm.md
---

# feat: 종목평가 추정실적 "추정 손익"(output2) 표 가독성 개선

gate: docs/gates/2026-07-12-stock-eval-estimate-income-table-gates.md

## Overview

종목평가 > 추정실적 탭 > "추정 손익" 섹션(KIS `estimate-perform` output2) 표를 3줄 압축 + 증감률 인라인(색) + 조 단위 + 오른쪽 정렬 + 추정치(E) 음영으로 재구성한다. **프론트 표시 방식만** 변경하고 백엔드는 손대지 않는다. (brainstorm: docs/brainstorms/2026-07-12-stock-eval-estimate-income-table-brainstorm.md, issue #82)

## 확정된 결정 (brainstorm 승인)

| 항목 | 결정 |
|------|------|
| 구조 | 6행 → 3줄(매출액·영업이익·당기순이익), 증감률을 각 값 아래 인라인 |
| 단위 | 억 기본 유지(기존 억/조 토글로 조 전환) — work 중 결정 변경. 사유 아래 참조 |
| 정렬 | 숫자 오른쪽 정렬(tabular-nums) |
| 증감률 색 | 증가=빨강(▲) / 감소=파랑(▼) |
| 추정치 | E 열 음영 |
| 범위 | 추정 손익(output2)만. 기본정보·추정 투자지표(output3)는 기존 유지 |
| 절차 | documented workflow (issue #82, worktree `feat/issue-82-estimate-income-table`) |

## 결정 변경 (work 중, 사용자 승인)

- **단위 기본**: brainstorm에서 "조 기본"으로 정했으나, `amountUnit`(억/조)이 **추정 손익 표와 재무 탭(대차/손익)이 공유하는 전역 상태**임을 work 중 확인(stock-eval.js:13, HTML 146·189의 토글이 동일 필드). 전역 기본을 조로 바꾸면 재무 탭 시작값도 바뀌어 "범위 밖: 재무 탭 변경"과 충돌.
- 태형님 결정: **억 기본 유지**. 추정 표는 재설계만 적용(첫 화면 억, 토글로 조 전환). 단위 헤더 라벨은 `amountUnit`에 따라 억원/조원 자동 표기.

## 현재 구조 (실측)

- 백엔드가 "추정 손익" 섹션을 6행 표로 전달:
  - `table.columns` = `["항목","2023.12","2024.12","2025.12","2026.12E","2027.12E"]`
  - `table.rows` = 매출액(억원)/매출액 증감률(%)/영업이익(억원)/영업이익 증감률(%)/당기순이익(억원)/당기순이익 증감률(%) × 기간값
  - 증감률 행은 백엔드에서 이미 ÷10 정규화됨(`StockEvaluationService.metricTable` 309).
- 프론트: `stock-eval.html` 202-224가 모든 섹션을 동일 generic 표로 렌더. 셀 포맷 `stock-eval.js` `fmtEstimateCell`(373)/`fmtAmountByUnit`(357)/`fmtEstimateLabel`(378).

## Proposed Solution — 변경 대상 (프론트 전용, 백엔드 0)

```
static/js/components/
└── stock-eval.js      # [수정] "추정 손익" 재구성 헬퍼(그룹/기간/증감률 포맷) 추가. 기존 포맷 함수 유지
static/partials/
└── stock-eval.html    # [수정] 추정실적 x-for 내부에서 title==='추정 손익'이면 전용 표, 그 외 기존 generic 표
```

- 백엔드/DTO/엔드포인트/Entity 변경 없음. `estimate.sections` 데이터는 그대로 사용.

## 구현 설계

### stock-eval.js — 헬퍼 추가 (순수 표시 로직)

- `estimateIncomeGroups(sec)`: 6행을 (금액행 + 다음 증감률행) 짝으로 묶어 3그룹 반환.
  - `metric` = 금액행 라벨에서 `(억원)` 제거(예: "매출액").
  - `cells[j]` = `{ amt, chg, up }` — `amt`=금액행 기간값, `chg`=증감률행 기간값, `up`=parseFloat(chg)>=0.
- `estimateIncomePeriods(sec)`: `columns[1..]`를 `{ label, est }`로 변환. `est`=`/E$/`, `label`="2026.12E"→"2026".
- `fmtChangePct(v)`: `Math.abs(parseFloat(v)).toFixed(1)+'%'` (빈값/NaN 방어).
- 금액은 기존 `fmtAmountByUnit(amt)` 재사용(억/조 토글 반영), 단위 헤더 라벨은 `amountUnit`으로 "조원/억원".
- 증감률 색은 Tailwind 클래스(증가 `text-red-500` / 감소 `text-blue-500`) — 작업 시 파일 내 기존 상승/하락 색 관례 grep 후 일치.

### stock-eval.html — 추정실적 표 분기

- `x-for="(sec, si) in sections"` 내부에서 `sec.title`로 분기:
  - `x-if="sec.title === '추정 손익'"` → 전용 표(3줄, 오른쪽 정렬, E열 음영, 증감률 색).
  - `x-else`(별도 template) → 기존 generic 표 유지(기본정보·추정 투자지표).
- 전용 표 헤더: 1열 "단위: 조원/억원", 이후 기간열(E는 음영 + E 배지).
- 전용 표 바디: 3행. 1열 지표 라벨(가운데/좌측), 각 기간 셀에 금액(상단, `fmtAmountByUnit`) + 증감률(하단, 색+▲/▼).

## Implementation Phases

### Phase 1 — 헬퍼 (stock-eval.js) — 완료
- [x] `estimateIncomeGroups`/`estimateIncomePeriods`/`fmtChangePct`/`isEstimateIncomeSection`/`changeClass`/`estimateUnitLabel` 추가. 기존 함수 무변경.
- [x] 상승/하락 색: 파일 관례(format.js 22-24: 증가 ▲ text-red-500 / 감소 ▼ text-blue-500 / 0 gray) 확인 후 그대로 따름.

### Phase 2 — 마크업 (stock-eval.html) — 완료
- [x] 추정실적 x-for 내부 `title === '추정 손익'` 분기(전용 표 / generic 표). 태그 균형 검증(div 101/101, template 55/55 등).
- [x] 전용 표 헤더/바디 구현(오른쪽 정렬·tabular-nums·E 음영·E 배지·증감률 색·단위 라벨).

### Phase 3 — 검증 — 완료
- [x] 라이브 브라우저(하네스: 실제 헬퍼+마크업+삼성전자 실데이터) 렌더 확인: 3줄(매출/영업/순이익), E열 음영·배지, 증감률 색/부호(▲빨강/▼파랑). 콘솔 에러 0.
- [x] 억/조 토글 전환 확인: 억=`2,589,355`(단위: 억원), 조=`258.9`(단위: 조원). 금액·단위 라벨 동기.
- [x] 기본정보·추정 투자지표 섹션 generic 표 그대로(회귀 없음) 확인.
- [x] 행수 불일치 폴백: `isEstimateIncomeSection`가 rows>=2 && 짝수만 통과 → 그 외 generic. 기본정보(2행)·투자지표가 generic 분기로 정상 렌더되어 폴백 경로 확인.

## Testing Strategy

- 수동 검증(실앱) 우선. 필요 시 seed 데이터로 격리 렌더 확인.
- 회귀: 같은 탭의 output3(추정 투자지표)·기본정보 표가 이전과 동일한지 대조.

## Risks / Trade-offs

- **행 짝짓기 가정**: output2가 (금액,증감률) 순서 6행이라는 가정 → 행수 홀수/불일치 시 generic 폴백으로 방어.
- **색 관례 불일치**: 국내 관례(증가 빨강)와 프로젝트 기존 색 충돌 가능 → 기존 파일 색 관례 확인 후 통일.
- **다크모드**: Tailwind 색 클래스가 앱 테마와 맞는지 확인(현 앱은 라이트 기준 클래스 사용 중).
- **범위 팽창 금지**: output3·기본정보·타 탭은 이번 범위 밖.

## Approval Gates (이 작업)

- Entity/백엔드/엔드포인트/레이어/API 변경 없음 → 추가 승인 게이트 없음(본 플랜 승인으로 work 진행).

## Out of Scope

- 백엔드 API/로직/DTO 변경.
- output3(추정 투자지표) 라벨·스케일 수정(별건 이슈 검토).
- 기본정보 섹션, 재무·신용·일정 탭 변경.
