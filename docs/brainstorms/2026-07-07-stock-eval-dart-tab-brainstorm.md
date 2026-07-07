# 종목평가 KIS/DART 탭 분리 + DART 재무상세 재사용 - Brainstorm

**Date:** 2026-07-07
**Status:** Decided
**Branch:** `feat/financial-statement-account-tree` (사용자 선택: 같은 브랜치 계속)

> 추가 확정(사용자): DART 탭 내부는 **서브탭 분리**(`연도별 추세` | `공시`), 최초 진입 조회는 **조회 버튼**(포트폴리오 타임라인과 동일 UX), 재사용 방식은 **옵션 A**(컨텍스트 파라미터화 + 공용 파티션).

## What We're Building

종목평가(`종목 평가`) 화면을 **상위 탭 2개**로 분리한다.

- **한국투자증권 탭**: 지금의 KIS 기반 화면(재무·추정실적·신용·일정 서브탭) 그대로.
- **DART 탭**: 방금 구현한 **연도별 추세**(요약 차트 + 표 + 세부 계정 계층 트리 + 지표 4분류) + **공시 목록**을, 종목평가에서 선택한 종목으로 조회해 보여준다.

종목평가는 국내(KRX) 종목만 다루고 `stockEval.selected.stockCode`(6자리)를 이미 갖고 있어 DART 조회에 그대로 쓸 수 있다.

## 확정된 결정 (사용자 확인 완료)

| 항목 | 결정 |
|------|------|
| DART 탭 범위 | **연도별 추세 + 공시 목록** (차트·요약표·세부 계정 트리·지표 4분류 전부 + 공시 탭) |
| 브랜치 | **지금 브랜치 계속** (`feat/financial-statement-account-tree`) — worktree 분리 권장은 안내함 |

## 현재 상태

- **종목평가** (`stock-eval.html`, `stock-eval.js`): `stockEval.activeTab` = finance/estimate/credit/schedule (전부 KIS). 상위 탭 없음.
- **DART 연도별 추세 + 공시** (`portfolio-deposit-financial.html` 529+, `financial.js`): 포트폴리오 슬라이드 패널에 결합.
  - 종목코드: `portfolio.selectedStockItem?.stockDetail?.stockCode`에서 읽음.
  - 상태: `portfolio.timeline*`, `portfolio.disclosure*`, `portfolio.timelineExpanded*`, `portfolio._timelineCharts`.
  - 마크업: 포트폴리오 파티션 내부에 인라인.

## 핵심 재사용 과제 (실측)

| 과제 | 실측/근거 | 함의 |
|------|-----------|------|
| **portfolio 결합** | `financial.js`에서 `this.portfolio.timeline/disclosure` 참조 **42곳**, 관련 메서드 약 43개 | 종목코드·상태가 `portfolio`에 하드코딩 → 컨텍스트 분리 필요 |
| **canvas ID 충돌** | `timelineAmountChart`/`timelineRatioChart`/`timelineShareChart` 하드코딩 | 두 화면이 같은 DOM에 상주 → 종목평가는 **다른 canvas ID** 필요 |
| **파티션 상시 마운트** | 모든 파티션이 index.html `data-partial`로 **항상 DOM에 존재**(x-show 토글) | `portfolio.selectedStockItem`을 종목평가에서 세팅하면 **포트폴리오 슬라이드 패널이 열림** → 상태 오염 방식 불가 |
| **국가** | 종목평가는 KRX만 | DART(KR) 경로만 필요, SEC/해외 분기 불필요 |

## 재사용 방식 옵션 (플랜에서 확정)

### 옵션 A — 컨텍스트 파라미터화 + 공용 파티션 (권장)

- 타임라인/공시 상태를 "컨텍스트 객체"로 추상화. 렌더/쿼리 헬퍼가 `ctx`(상태 bag)와 stockCode를 받도록 변경.
  - 포트폴리오: `ctx = this.portfolio` (기존 동작 보존, 기본값으로 처리해 마크업 변경 최소화)
  - 종목평가: `ctx = this.stockEval.dart` (신규 상태 bag)
- 타임라인+공시 뷰 마크업을 **공용 파티션**으로 추출해 포트폴리오 패널과 종목평가 DART 탭이 함께 include. 파티션은 `ctx`와 canvas ID 접두어를 받음.
- canvas ID를 컨텍스트별 접두어로 유일화(예: `pf-timelineAmountChart` / `eval-timelineAmountChart`), 차트 인스턴스도 컨텍스트별 배열로 관리.
- 장점: 중복 없음, 단일 소스. 단점: ~40 참조 + 마크업 추출 churn, 기존 패널 회귀 검증 필요.

### 옵션 B — 호스트 리졸버 (`this._tlHost()`)

- `this.portfolio.timelineX` → `this._tlHost().timelineX`로 치환. `_tlHost()`가 활성 컨텍스트(포트폴리오/종목평가)를 반환.
- 활성 컨텍스트 판별을 명시 플래그로 관리(진입 시 set). 마크업 바인딩 변경은 적으나 판별 로직이 상태를 가짐(암묵적).
- 장점: 마크업 변경 최소. 단점: "활성" 판별이 전역 상태라 취약, 동시 렌더 가정에 의존.

### 옵션 C — 최소 중복

- 종목평가에 타임라인/공시 QUERY만 얇게 복제(eval 상태에 write), 순수 RENDER 헬퍼(~15개)만 `ctx` 인자로 공유, 마크업은 별도.
- 장점: 기존 포트폴리오 코드 최소 변경. 단점: 쿼리·마크업 일부 중복.

> 성향: **옵션 A**(단일 소스·유지보수 우위). 단 회귀 리스크가 있으니 플랜에서 마크업 추출 경계와 canvas/차트 스코프를 확정하고, 기존 포트폴리오 패널 동작을 스냅샷 검증한다.

## UI 구성 (제안)

- 종목평가 요약 카드 아래 **상위 탭**: `한국투자증권` | `DART` (`stockEval.provider` = 'kis' | 'dart').
- `한국투자증권` 선택 시: 기존 서브탭 바(재무/추정/신용/일정) + 내용.
- `DART` 선택 시: 서브탭 `연도별 추세` | `공시` (또는 세로 스택). 각각 기존 뷰 재사용.
- 종목 전환/재검색 시 두 컨텍스트 상태·차트 정리(reset). DART 탭 최초 진입 시에만 조회(빈 상태 → 조회 버튼 or 자동조회는 플랜에서).

## Edge Cases

- **차트 orphan/충돌**: 컨텍스트별 canvas ID + 차트 배열 분리. 탭/종목 전환 시 destroy.
- **종목 전환**: `stockEvalSelect`에서 `stockEval.dart` 상태·차트 리셋.
- **DART 미지원/데이터 없음**: 우선주만/신규 상장 등 → 기존 타임라인 빈 상태 처리 재사용.
- **포트폴리오 패널 회귀**: 기본 컨텍스트=portfolio로 기존 동작 100% 보존 확인.
- **KRX 전용**: SEC 분기 불필요하나, 종목평가가 이미 KRX 필터라 안전.
- **성능**: DART 탭 열 때만 조회(탭 지연 로딩). KIS 탭엔 영향 없음.

## Open Questions

1. DART 탭 내부 구성: `연도별 추세`/`공시`를 **서브탭**으로 둘까, 세로로 이어서 보여줄까? (권장: 서브탭)
2. DART 탭 최초 진입 시 **자동 조회** vs **조회 버튼**? (포트폴리오는 조회 버튼. 종목평가 KIS 탭은 자동. 일관성 검토 — 권장: 연 수/개별·연결 기본값으로 자동 조회)
3. 재사용 방식 A/B/C 중 최종 선택(권장 A).

## 범위 밖 (하지 않음)

- 한국투자증권 탭 내부 로직/화면 변경(그대로 유지).
- 해외(SEC) 종목평가 지원.
- 백엔드 API 신규/변경(기존 timeline·disclosure API 재사용, 무변경).
- 계정 트리/증감/강조 로직 변경(이미 완료분 재사용).
</content>
