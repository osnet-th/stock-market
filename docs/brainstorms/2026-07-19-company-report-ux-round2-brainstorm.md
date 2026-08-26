# 기업 리포트 UX 개선 2차 - Brainstorm

**Date:** 2026-07-19
**Status:** Decided (태형님 확인 완료, 2026-07-19 — AskUserQuestion 2건)
**Gate:** docs/gates/2026-07-19-company-report-ux-round2-gates.md
**선행 기능:** 기업분석리포트 #81 / 기업 리포트 입력 개선 #84 (docs/brainstorms/2026-07-19-company-report-input-improvements-brainstorm.md)

## 배경

기업분석리포트(#81, #84) 사용 중 태형님이 세 가지 불편을 지적.

1. **주가지표 자동/내 계산 분리** — 값이 누락돼 사용자가 찾아 입력해도, "자동"은 그 값을 반영하지 않고 `—`/옛 값을 그대로 보여 준다. "자동"과 "내 계산"이 한 화면에 병기돼 헷갈림. 누락 시 `—`보다 사용자가 입력한 값을 쓰는 게 맞음. → 필드를 나누지 말고 **하나의 표에서 처음엔 자동으로 보여주고 그 칸을 사용자가 수정**하는 형태로.
2. **DART 원문 이동 불편** — 리포트 작성 중 누락 항목을 확인하려면 종목평가로 이동해 DART 문서를 찾아야 함. → 최상단에 해당 종목 **10년치 DART 정기보고서(사업·분기·반기)를 원문 바로가기**로.
3. **경쟁사 비교 여러 줄 입력** — 경쟁사 "비교 내용"도 여러 줄 입력이 가능하면 좋겠음.

## 확정된 결정 (태형님 확인 완료, 2026-07-19)

| # | 항목 | 결정 |
|---|------|------|
| 범위 | Issue/worktree 단위 | **1개 이슈 3작업** — 하나의 Issue·worktree에서 ①②③ 순차 진행 |
| ① | 주가지표 편집 방식 | **재료를 셀에서 인라인 편집** — 하나의 표에 지표 나열, 재료(주가·유통주식수·당기순이익·자본총계·매출액·영업CF)를 표 안에서 인라인 편집. 자동값은 기본 표시, 수정 시 즉시 파생지표 재계산. EV/EBITDA·ROIC·발생액/총자산은 재료 매핑이 없어 자동 전용 표시 |

## 현재 구조

| 대상 | 위치 | 현재 |
|------|------|------|
| 주가지표(자동) 그리드 | `company-report.html:475-495`(작성 5단계), `:1197-1215`(상세 6번) | `preview/detail.snapshot.priceMetrics` 11개 카드. 누락 시 `—` |
| 주가지표 계산기(내 계산) | `company-report.html:496-617`(작성 5단계), `:1221-1250`(상세, `crHasCustomMetrics` 조건부 파란 블록) | 재료 6칸 입력 + 파생 표. **자동 그리드와 별도**로 병기 |
| 계산 엔진 | `company-report.js:876 crMetricBase`, `:907 crCalcMetrics`, `:844 _crPerShare` | 이미 "입력값 우선 → 자동 폴백". `metricInputs = {price, shares, netIncome, equity, revenue, operatingCf, (legacy eps/bps)}` (schemaVersion 2) |
| 공시 조회 API | `api.js:408 getDisclosures`, `DisclosureQueryService`, `DisclosureResponse(reportName, viewerUrl…)` | `/api/stocks/{code}/disclosures?fromDate&toDate&types=A` → 정기공시 + DART 원문 `viewerUrl` 반환. **작성 화면에는 미노출** |
| 경쟁사 비교 `competitors(name, segment, note)` | `company-report.html:369-375`(편집), `:1093-1099`(상세) | `note`가 단일행 `<input>`(500자). 상세 셀은 `whitespace` 미적용 |

## 개선 방향 (초안 — 상세 설계는 plan에서)

### ① 주가지표 자동/내 계산 통합 (재료 인라인 편집)
- 자동 그리드 + 계산기 두 블록을 **하나의 표**로 통합. 각 지표를 한 행으로 나열하고, 그 지표를 만드는 **재료를 표 안에서 인라인 편집**.
- 재료(리프) 값: `price(주가)`, `shares(유통주식수)`, `netIncome(당기순이익)`, `equity(자본총계)`, `revenue(매출액)`, `operatingCf(영업CF)`. — #84에서 이미 공식형 입력 구조로 전환됨.
- 파생 지표(시가총액·EPS·BPS·PER·PBR·PSR·PCFR·PER×PBR)는 `crCalcMetrics`/`crMetricBase` 그대로 재사용 — 재료 편집 시 연쇄 재계산.
- 자동값은 회색 기본 표시(placeholder/prefill), 사용자가 고치면 그 값이 표시·계산에 사용. 누락(`—`)이던 값도 입력하면 반영.
- **자동 전용 행**: EV/EBITDA(근사)·ROIC(근사)·발생액/총자산 — 재료 매핑이 없어 스냅샷 자동값만 표시(편집 불가).
- 적용 위치: **작성 5단계 + 상세 6번 섹션 둘 다** 통합.
- 상세 뷰의 조건부 파란 "내 계산" 블록(`crHasCustomMetrics`)은 통합 표로 흡수.

### ② DART 10년 정기보고서 바로가기
- **프론트 전용.** 백엔드·API 무변경(`getDisclosures` 재사용).
- 종목 선택/리포트 로드 시 `types=A`(정기공시), 기간 10년으로 조회 → `reportName`이 사업보고서/분기보고서/반기보고서인 항목만 필터 → 최상단 **링크 바**로 렌더(최신순/연도 그룹), `viewerUrl` 새 탭.
- `financial.js`의 `_disclosureFromDate`/`_formatYmd`/`formatDisclosureDate` 로직 참고(중복 없이 company-report 전용 헬퍼로 최소 구현).

### ③ 경쟁사 비교 "비교 내용" 여러 줄
- 편집: `note` `<input>` → `<textarea>`(rows 2, resize-y). 레이아웃 `items-center` → `items-start`.
- 상세: note 셀에 `whitespace-pre-wrap` 추가(줄바꿈 보존).
- 백엔드/JS 무변경(`_crBuildManual`의 `rows()` trim은 내부 줄바꿈 보존).

## Edge Cases
- 통합 표: 재료 일부만 입력(예: shares만) 시 파생지표 부분 재계산 — 기존 `_crPerShare` 우선순위(input→legacy→auto numerator→snapshot) 유지, 값 없으면 `—`.
- 통합 표: 당기순이익·자본총계 음수(적자·자본잠식) 입력 유지(`crSignedAmountInput`). shares=0/음수 방어.
- 통합 표: 구 스키마(legacy eps/bps 직접 입력) 리포트를 새 통합 UI로 열 때 값 손실 없이 표시.
- DART 링크: 10년 범위 정기공시만이라 단일 페이지(≈40건, DART 100건/page 이내)로 충분한지 확인. 정정 보고서(remark) 표기.
- DART 링크: 비상장/신규 상장 등 10년 미만 종목 — 있는 만큼만.
- 경쟁사: 여러 줄 입력 후 기존 저장 리포트(단일행) 조회 정상.

## 범위 밖 (하지 않음)
- 백엔드 스냅샷 산출 로직(`SnapshotFinancialExtractor`) 및 주가지표 자동 계산 변경.
- 공시 조회 백엔드/신규 API — 기존 `getDisclosures` 재사용만.
- `metricInputs` 저장 스키마 변경 — 이미 v2(netIncome/equity 보유). **UI 통합만**, Entity·record 무변경.
- 투자판단·재무지표·청산가치·DCF 섹션 동작 변경.

## Open Questions (plan에서 확정)
1. **①** 통합 표에서 "재료 미입력(자동)" 상태의 EPS/BPS 표시 기준 — 보고 기준값(`pm.eps/pm.bps`) vs 계산값(`재료÷주식수`). `_crPerShare` 우선순위 그대로 채택할지.
2. **①** 자동 전용 행(EV/EBITDA·ROIC·발생액)을 통합 표에 함께 둘지, 별도 소섹션으로 분리할지.
3. **②** DART 링크 바 노출 위치(작성 위저드 상단 / 상세 상단 / 둘 다), 표시 형태(연도 그룹 접이식 vs 단순 최신순 리스트), 로드 시점(종목 선택 즉시 vs 버튼).
