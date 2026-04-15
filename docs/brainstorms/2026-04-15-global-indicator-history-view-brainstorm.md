# Brainstorm: 글로벌 경제지표 히스토리 조회 UI

**Date:** 2026-04-15
**Status:** Decided

## What We're Building

글로벌 경제지표(TradingEconomics)의 히스토리 데이터를 표/그래프로 조회하는 기능.
현재 `global_indicator` 테이블에 히스토리가 적재되고 있지만, 조회 API와 UI가 없는 상태.

### 핵심 요구사항
- ECOS(국내 경제지표)의 표/그래프 토글 패턴을 동일하게 적용
- **지표타입(IndicatorType) 기준**으로 히스토리 조회
- **표**: 지표타입별 테이블 — 행=국가, 열=cycle(시계열)
- **그래프**: 지표타입별 Chart.js 라인차트 — 국가별 라인(색상 구분)

## Why This Approach

### 선택: 지표타입별 그룹 뷰
- 글로벌 지표의 핵심 가치는 **국가 간 비교** → 같은 지표를 국가별로 한눈에 비교할 수 있어야 함
- ECOS 패턴(표/그래프 토글)을 유지하여 UX 일관성 확보
- 차트 수 = 지표타입 수 (관리 가능한 수준)

### 기각: ECOS 1:1 미러링
- 국가×지표타입 조합으로 차트가 폭발적으로 증가
- 국가 간 비교 불가

### 기각: 하이브리드 (드릴다운)
- YAGNI — 현재 단계에서 불필요한 복잡도

## Key Decisions

1. **UI 패턴**: ECOS와 동일한 표/그래프 토글
2. **조회 단위**: 지표타입(IndicatorType) 기준
3. **표 구성**: 행=국가, 열=cycle(시계열), 지표타입별 테이블
4. **그래프 구성**: 지표타입별 1개 차트, 국가별 라인(색상 구분)
5. **기존 패턴 활용**: Chart.js, Alpine.js, generation counter, LRU 캐시 등

## Scope

### In Scope
- 글로벌 히스토리 조회 API (GlobalIndicatorRepository에 히스토리 쿼리 추가)
- 글로벌 히스토리 응답 DTO
- 프론트엔드 표/그래프 토글 UI
- ECOS와 동일한 기술 패턴 적용

### Out of Scope
- 스프레드 분석 카드 (글로벌용)
- 국가 필터링/선택 기능
- 드릴다운 상세 뷰

## Open Questions

없음 — 모든 주요 결정 완료.