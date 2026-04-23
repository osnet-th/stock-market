---
date: 2026-04-21
topic: global-favorite-realtime-indicator
issue: "#28"
---

# 글로벌 관심 경제지표 실시간 조회 전환

## What We're Building

대시보드에서 **글로벌 관심 경제지표**를 조회할 때, 현재 `GlobalIndicatorLatest` DB 테이블에 의존하는 경로를 **글로벌 경제지표 탭과 동일한 스크래핑(Trading Economics, 캐시 경유)** 경로로 전환한다.

핵심 변경: 관심 지표가 속한 **카테고리 단위로 실시간 일괄 조회** 후, 사용자의 관심 지표만 필터링하여 반환한다. 스크래핑 실패 지표는 "조회 실패" 상태로 노출하고, 사용자가 수동으로 재조회할 수 있는 UI를 제공한다.

## Why This Approach

- **현상**: 대시보드의 관심 글로벌 지표는 `GlobalIndicatorLatest` DB 테이블을 전체 로드한 뒤 `indicatorCode` 기준으로 in-memory join 한다. DB에 데이터가 없거나 stale한 경우 "데이터 없음"으로 표시된다.
- **이슈 요구사항**: 글로벌 경제지표 탭은 `GlobalIndicatorCacheService` → `TradingEconomicsIndicatorAdapter` 경로로 실시간 스크래핑을 수행하고 있으며, 동일한 소스를 관심 지표 조회에도 재사용해야 한다.
- **접근 선택 근거**: 지표별 개별 호출보다 **카테고리 일괄 조회 후 필터링**이 Caffeine 캐시 히트를 극대화하고, 탭 조회와 관심 지표 조회의 캐시가 공유되어 효율적이다.

## Key Decisions

- **조회 경로**: `GlobalIndicatorCacheService` 재사용 (카테고리 단위 일괄 조회) — 글로벌 탭과 동일 소스, 캐시 공유
- **스코프**: `FavoriteIndicatorSourceType.GLOBAL`만 이번 이슈에 포함. ECOS(국내)는 현재 정상 동작이므로 제외
- **조회 단위**: 관심 지표가 속한 **카테고리들만** 일괄 조회 후, 사용자의 관심 지표 항목만 필터링
- **실패 처리**: 지표별로 "조회 실패" 상태를 표시하고, 해당 지표를 다시 불러올 수 있는 **재조회 아이콘**을 UI에 추가 (재조회 시 캐시 우회 또는 invalidate 후 재시도)
- **DB 레거시**: `GlobalIndicatorLatest` 테이블 및 Writer 경로는 **이 이슈에서 손대지 않음**. 관심 지표 Read 경로만 스크래핑으로 전환하고, Writer/테이블은 다른 용도 가능성을 감안해 유지

## Open Questions

모두 해결됨. (구현 디테일은 `/ce:plan` 단계에서 다룸)

## Resolved Questions

- "실시간 조회"의 의미 → 글로벌 탭과 동일한 스크래핑(캐시 경유)
- N개 관심 지표 조회 전략 → 관심 지표가 속한 카테고리 일괄 조회 후 필터링
- 수정 스코프 → 글로벌만 (ECOS 제외)
- 스크래핑 실패 UX → 지표별 "조회 실패" + 재조회 아이콘
- DB `GlobalIndicatorLatest` 처리 → 유지 (이 이슈에서 손대지 않음)

## Next Steps

→ `/ce:plan` 으로 구현 계획 수립