---
title: "feat: 종목평가 KIS/DART 탭 분리 + DART 재무상세 재사용"
type: feat
status: active
date: 2026-07-07
origin: docs/brainstorms/2026-07-07-stock-eval-dart-tab-brainstorm.md
---

> **접근 확정(사용자): "하나로 합쳐서 호출".** 타임라인/공시 로직·마크업을 **단일화**하고 포트폴리오·종목평가가 각자 컨텍스트로 호출한다.
> 회귀 방지 설계: 메서드를 `this.portfolio.X` → `ctx.X`로 바꾸되 **필드명을 포트폴리오 기존 이름 그대로** 유지 →
> 포트폴리오는 `ctx = this.portfolio`를 넘겨 **동작·레이스가드 불변**, 종목평가는 동일 필드명을 가진 `ctx = this.stockEval.dart`를 넘김.
> canvas는 `ctx.canvasPrefix`(`pf-`/`eval-`)로 유일화. 마크업은 공용 파티션으로 추출해 양쪽에서 include.

# feat: 종목평가 KIS/DART 탭 분리 + DART 재무상세 재사용

## Overview

종목평가 화면에 상위 탭 `한국투자증권` | `DART`를 추가한다. 한국투자증권 탭은 기존 KIS 화면(재무/추정/신용/일정) 그대로, DART 탭은 **연도별 추세**(차트+요약표+세부 계정 계층 트리+지표 4분류)와 **공시 목록**을 서브탭으로 제공한다. DART 조회는 종목평가에서 선택한 `stockEval.selected.stockCode`(KRX 6자리)로 수행하며, **조회 버튼** 방식이다. (brainstorm: docs/brainstorms/2026-07-07-stock-eval-dart-tab-brainstorm.md)

## 확정된 결정 (brainstorm 승인)

| 항목 | 결정 |
|------|------|
| 상위 탭 | `한국투자증권`(기존) / `DART`(신규) |
| DART 내부 | 서브탭 `연도별 추세` \| `공시` |
| 최초 조회 | 조회 버튼(포트폴리오 타임라인과 동일 UX) |
| 재사용 방식 | 옵션 A(로직 단일화 + 컨텍스트 분리) |
| 범위 | 연도별 추세 + 공시. 백엔드 무변경 |
| 브랜치 | `feat/financial-statement-account-tree` 계속 |

## 재사용 아키텍처 (핵심)

현재 타임라인/공시 로직은 `financial.js`에서 `this.portfolio.timeline*/disclosure*`를 42곳 참조하고 canvas ID가 하드코딩돼 포트폴리오 패널에 결합돼 있다. 이를 **컨텍스트(상태 bag) + 종목코드 getter + canvas 접두어**로 분리해 포트폴리오와 종목평가가 같은 로직을 공유한다.

- **로직 단일화**: 타임라인/공시 메서드를 "컨텍스트를 받는" 형태로 재작성. 포트폴리오 기존 메서드는 **같은 이름의 얇은 위임(delegator)**으로 유지 → 포트폴리오 마크업 무변경, 회귀 위험 최소화.
  - `runTimelineQuery()` → `this._runTimeline(this.portfolio, () => portfolioStockCode())`
  - eval: `this._runTimeline(this.stockEval.dart, () => this.stockEval.selected?.stockCode)`
- **상태 분리**: `stockEval.dart` 신규 bag(timeline/disclosure/expanded/charts/gen + `subTab` + `canvasPrefix:'eval-'`). 포트폴리오는 기존 `portfolio.*` 유지.
- **canvas 유일화**: 차트 canvas id를 컨텍스트 접두어로 생성(`pf-timelineAmountChart` / `eval-timelineAmountChart`), 차트 인스턴스 배열도 컨텍스트별(`ctx._timelineCharts`). 파티션 상시 마운트로 인한 ID 충돌 방지.
- **마크업**: 종목평가 DART 탭 마크업은 포트폴리오 타임라인/공시 뷰를 참고해 **eval 컨텍스트·eval canvas id로 바인딩**해 추가(공용 파티션 추출은 회귀 위험 대비 이득이 적어, 이번엔 eval 마크업을 별도로 두고 로직만 공유 — Phase 1에서 최종 결정).

> 원칙: 동작 중인 포트폴리오 패널을 깨지 않는다("요청 없는 리팩토링 지양"). 메서드는 위임으로 이름/시그니처를 보존하고, 마크업은 무변경을 목표로 한다.

## Proposed Solution — 변경 대상 (프론트 전용, 백엔드 0)

```
static/js/components/
├── stock-eval.js                    # [수정] stockEval.provider('kis'|'dart') + stockEval.dart 상태 + eval 위임 메서드/서브탭 제어
├── financial.js                     # [수정] 타임라인/공시 메서드를 컨텍스트 인자 기반 내부구현 + 기존 이름 위임. canvas id 접두어화
static/partials/
├── stock-eval.html                  # [수정] 상위 탭(KIS/DART) + DART 탭(연도별 추세/공시 서브탭 + 뷰 마크업)
└── portfolio-deposit-financial.html # [수정 최소] canvas id 접두어 반영(pf-) — 그 외 무변경 목표
```

- 백엔드/DTO/엔드포인트 변경 없음. 기존 `/financial/timeline`, `/disclosures` API 재사용.
- Entity·레이어·의존성 방향 변경 없음.

## Implementation Phases

### Phase 1 — 로직 컨텍스트화 (financial.js) — 완료
- [x] 타임라인/공시 메서드를 `ctx`(상태 bag, 트레일링 기본값 `this.portfolio`) 기반으로 재작성. 종목코드 `_ctxStockCode(ctx)`, canvas `_canvasId(ctx, base)`.
- [x] 포트폴리오 마크업 **무변경**(ctx 생략 → 기본 portfolio). 필드명 보존으로 레이스가드 불변.
- [x] canvas id를 `(ctx.canvasPrefix||'') + base`로 생성 → 포트폴리오 `timelineAmountChart`, eval `eval-timelineAmountChart`. 차트 배열 `ctx._timelineCharts`.
- [x] 포트폴리오 default-ctx 경로 harness 검증(무인자 호출 정상, canvas 미접두어).

### Phase 2 — 종목평가 상태·제어 (stock-eval.js) — 완료
- [x] `stockEval.provider`('kis') + `stockEval.dart` bag(포트폴리오와 동일 필드명 + canvasPrefix:'eval-') 추가.
- [x] `stockEvalSelect`에서 provider='kis' 리셋 + `_stockEvalResetDart(stockCode)`로 dart 상태·차트 리셋·종목코드 설정.
- [x] `setEvalProvider`/`setEvalDartSubTab` 제어. 조회는 공용 `runTimelineQuery(stockEval.dart)`/`runDisclosureQuery(stockEval.dart)` 직접 호출.

### Phase 3 — 종목평가 마크업 (stock-eval.html) — 완료
- [x] 요약 카드 아래 상위 탭(한국투자증권/DART) 바 + KIS 래퍼(`x-show provider==='kis'`).
- [x] DART 탭: 서브탭(연도별 추세/공시) + 조회 컨트롤(기간·연결/개별·조회) + 타임라인 뷰(요약표·차트3·세부 계정 트리·지표4) + 공시 뷰. `stockEval.dart` 바인딩, `eval-` canvas id.

### Phase 4 — 검증 — 완료
- [x] harness(실 데이터 seed): DART 탭 연도별 추세 렌더 — 요약표·partial 앰버·금액차트(eval- canvas)·계정 트리(유동자산→현금및현금성자산)·지표4·YoY 64개 화살표. 콘솔 에러 0.
- [x] 컨텍스트 격리 검증: portfolio canvas=`timelineAmountChart` / eval=`eval-timelineAmountChart`, 토글 상태 각 ctx에 분리.
- [x] 포트폴리오 default-ctx 무변경 검증(무인자 호출·미접두어 canvas).
- [x] 구조 sanity: financial.js 380/380 braces, stock-eval.html div 99/99.

## Testing Strategy

- 수동 검증(실앱) 1차. 격리 harness로 렌더/차트 유일성 확인 가능.
- 회귀 최우선: 포트폴리오 패널의 연도별 추세·공시가 이전과 동일한지 대조.

## Risks / Trade-offs

- **포트폴리오 패널 회귀**: 최대 리스크. 위임 패턴으로 이름/시그니처 보존 + 회귀 검증으로 방어.
- **canvas ID 충돌**: 파티션 상시 마운트 → 반드시 컨텍스트 접두어. 미적용 시 차트가 엉뚱한 캔버스에 그려짐.
- **마크업 중복**: eval 마크업을 별도로 두면 향후 뷰 변경이 두 곳. 공용 파티션 추출은 이득/위험 저울질 후 Phase 1에서 재검토.
- **상태 누수**: eval/portfolio 상태 완전 분리. `_financialRequestGeneration`류 공유 카운터는 컨텍스트별로 분리.
- **범위 팽창**: 이번엔 KRX+DART 한정. 해외/SEC 종목평가는 범위 밖.

## Approval Gates (이 작업)

1. **동작 중 포트폴리오 타임라인/공시 로직 구조 변경**(위임 리팩터) — public 화면 동작 보존이 전제. → **본 플랜 승인 시 진행**.
- Entity/백엔드/엔드포인트/레이어 변경 없음.

## Out of Scope

- 한국투자증권 탭 내부 화면/로직 변경.
- 해외(SEC) 종목평가.
- 백엔드 API 신규/변경.
- 계정 트리/증감/강조 로직 변경(재사용만).
</content>
