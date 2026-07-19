# 기업 리포트 입력 개선 - Brainstorm

**Date:** 2026-07-19
**Status:** Decided (태형님 확인 완료, 2026-07-19)
**Gate:** docs/gates/2026-07-19-company-report-input-improvements-gates.md
**선행 기능:** 기업분석리포트 (#81, docs/brainstorms/2026-07-12-company-analysis-report-brainstorm.md)

## 배경

기업분석리포트(#81) 사용 중 태형님이 정성 입력과 주가지표 계산기에서 불편함을 지적. 실제 회사 홈페이지/공시 내용을 옮겨 담기에 입력 폼이 부족하고, 주가지표 계산기는 일부 값(예: 유통주식수)만 바꿔도 파생지표를 손으로 다시 계산해야 함.

## 확정된 결정 (태형님 확인 완료, 2026-07-19)

| # | 항목 | 결정 |
|---|------|------|
| 1 | 연혁 내용 여러 줄 입력 | 단일행 `<input>` → `<textarea>`(여러 줄) |
| 2 | 연혁 날짜 년/년-월 | `year`를 `2024` 또는 `2024.03`(년 또는 년-월) 모두 허용 |
| 3 | 매입처 품목 칸 | 매입처에 **무엇을 매입하는지(품목/원자재명)** 적을 칸 추가 |
| — | (원래 4번) | 무시 (내용 없음) |
| B | 주가지표 공식화 | **파생지표 전체**(EPS·BPS·시가총액·PER·PBR·PSR·PCFR·PER×PBR)를 공식(분자÷분모) 형태로 보존, 유통주식수 등 안의 값만 고치면 연쇄 재계산 |

## 현재 구조

| 대상 | 위치 | 현재 |
|------|------|------|
| 연혁 `HistoryItem(year, content)` | `ReportManual.java:39`, `company-report.html:213-217` | year=`\d{4}` 고정, content=단일행 `<input>` (저장은 500자 가능) |
| 판매처/매입처 `PartnerItem(name, share, note)` | `ReportManual.java:42`, `company-report.html:249-274` | 판매처·매입처가 **동일 record 공유**. 무엇을 매입/판매하는지(품목) 칸 없음 |
| 주가지표 계산기 `MetricInputs(price, shares, eps, bps, revenue, operatingCf)` | `ReportManual.java:58`, `company-report.js:822-859`, `company-report.html:495-540` | eps·bps를 **계산된 최종 값 하나**로 직접 입력. 유통주식수만 바꿔도 eps를 손으로 다시 계산해 넣어야 함 |

## 개선 방향 (초안 — 상세 설계는 plan에서)

### 1. 연혁 내용 여러 줄
- `company-report.html`의 content `<input>` → `<textarea>`(rows 2~3, 자동 확장 선택). 조회 표시에서 줄바꿈 보존.
- 백엔드 무변경(이미 500자 저장). 필요 시 `FIELD_MAX_LENGTH` 상향 검토.

### 2. 연혁 날짜 년/년-월
- 백엔드 `requireYear`가 연혁 year에 `\d{4}` 강제(`ReportManual.java:146`, `:91`). → 연혁 전용으로 `YYYY` 또는 `YYYY.MM`(또는 `YYYY-MM`) 허용 패턴 신설.
- 프론트 입력 마스크(`company-report.html:214`의 4자리 절삭)를 연/월 허용으로 완화. 정렬(`company-report.js:551` 오름차순)은 문자열 비교로 `YYYY`·`YYYY.MM` 혼재해도 동작하도록 확인.
- **주의:** `financialChanges`·`revenueForecasts`의 year도 `requireYear`를 공유 → 연혁만 완화하고 나머지는 `\d{4}` 유지(분리 필요).

### 3. 매입처 품목 칸
- `PartnerItem`은 판매처·매입처 공유 record → 필드 추가 시 양쪽 노출. **설계 결정(Approval Gate):**
  - (A) `PartnerItem`에 `item`(품목) 필드 추가 → 판매처=공급 제품 / 매입처=매입 원자재로 라벨만 다르게. (대칭·단순, 권장 후보)
  - (B) 매입처에만 노출(판매처는 숨김).
- plan에서 A/B 확정. Entity(record) 수정 = Approval Gate.

### B. 주가지표 공식형 재계산 (파생지표 전체)
- **핵심:** eps·bps를 최종 값이 아니라 **분자(당기순이익·자본총계)** 로 입력받고, 공통 분모 **유통주식수**로 나눠 산출. 그러면 유통주식수만 고쳐도 EPS·BPS·시가총액·PER·PBR·PSR·PCFR·PER×PBR이 연쇄 재계산.
- 리프(편집 대상) 값: `price(주가)`, `shares(유통주식수)`, `netIncome(당기순이익)`, `equity(자본총계)`, `revenue(매출액)`, `operatingCf(영업CF)`.
- 파생 관계:
  - 시가총액 = price × shares
  - EPS = netIncome ÷ shares
  - BPS = equity ÷ shares
  - PER = price ÷ EPS, PBR = price ÷ BPS
  - PSR = 시가총액 ÷ revenue, PCFR = 시가총액 ÷ operatingCf
  - PER×PBR = PER × PBR
- UI: "내 계산" 표를 **공식 + 실제 대입값** 형태로 표시하고, 리프 값만 인라인 편집(예: `당기순이익 ÷ 유통주식수 = 5,300,000 ÷ 683 = 7,757`). 편집 시 즉시 재계산.
- **하위호환(Approval Gate):** 저장 스키마 `MetricInputs`의 eps·bps → netIncome·equity로 교체 시 `CURRENT_SCHEMA_VERSION`(현재 1) bump + 기존 저장 리포트 폴백(구 eps/bps 값이 있으면 그대로 사용, 없을 때만 새 공식). `ReportManualJsonConverter`·`ReportSnapshotJsonMapper` 영향 확인.

## Edge Cases
- 연혁 날짜: `2024`, `2024.03`, `2024-03` 혼재 입력 → 정렬·표시 일관성. 잘못된 형식(예: `2024.13`) 검증.
- 매입처 품목 필드 추가 후 기존 저장 리포트(품목 null) 조회 정상.
- 주가지표: netIncome/equity 결측 시 자동 스냅샷값(pm.eps/pm.bps) 폴백 유지. shares=0/음수 방어.
- 구 스키마(v1) 리포트를 새 UI에서 열 때 eps/bps 값 손실 없이 표시.

## 범위 밖 (하지 않음)
- 기업분석리포트의 다른 섹션(투자판단·재무지표·청산가치·DCF) 동작 변경.
- 판매처/매입처 자동 수집.
- 주가지표 자동 스냅샷 산출 로직(백엔드 `SnapshotFinancialExtractor`) 변경 — 계산기(내 계산) UI/입력 구조만 대상. (단, B의 폴백을 위해 스냅샷 EPS/BPS 참조는 유지)

## Open Questions (plan에서 확정)
1. 매입처 품목 칸: (A) 공유 필드 추가 vs (B) 매입처 전용. → plan 확정.
2. 연혁 날짜 구분자: `YYYY.MM` vs `YYYY-MM` 표기 통일.
3. `MetricInputs` 스키마 교체 방식: 필드 교체(eps→netIncome) vs 필드 병행(둘 다 유지). 하위호환 폴백 상세.
