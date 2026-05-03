---
date: 2026-05-03
module: favorite
problem_ref: .claude/analyzes/favorite/global-indicator-type-i18n/global-indicator-type-i18n.md
title: 글로벌 지표 한국어 라벨(displayName) 노출
---

# 글로벌 지표 한국어 라벨 노출

## 채택안 (안1)

DTO 에 한국어 라벨 필드를 추가하고, 프론트는 신규 필드를 사용해 노출.
- 영문 enum 식별자(`indicatorType`)는 키 용도로 그대로 유지 → 기존 호출 호환 유지.
- enum 의 `displayName` 을 그대로 노출 → 단일 소스(enum) 기준.

## 변경 사항

### 백엔드

**1. `EnrichedFavoriteResponse.GlobalItem`**
- 필드 추가: `String indicatorTypeDisplayName`
- 정상 케이스: `enriched.snapshot().getIndicatorType().getDisplayName()`
- noData 케이스: `parsed.indicatorType().getDisplayName()` (enum 파싱 성공 시) 또는 `parsedType` (실패 시)
- failed (INVALID_CODE) 케이스: enum 못 구하므로 `parsedType` (raw 문자열) 그대로
- failed (FETCH/PARSE) 케이스: `enriched.favorite()` 의 indicatorCode 파싱 시도 → 실패 시 raw

**구현 가이드** (`EnrichedFavoriteResponse.java:87-130`):
- `GlobalItem.from` 안에서 `displayName` 계산 헬퍼 도입:
  ```java
  String displayName = resolveDisplayName(parsedType);
  ```
- `resolveDisplayName(String parsedType)` static method:
  ```java
  try { return GlobalEconomicIndicatorType.valueOf(parsedType).getDisplayName(); }
  catch (IllegalArgumentException e) { return parsedType; }
  ```

**2. `RecentUpdateResponse.GlobalUpdate`**
- 필드 추가: `String indicatorTypeDisplayName`
- `from(GlobalIndicatorLatest)` 안에서 `latest.getIndicatorType().getDisplayName()`

### 프론트엔드 (`index.html`)

| 라인 | 변경 |
|---|---|
| 525 | `item.indicatorType.replace(/_/g, ' ')` → `item.indicatorTypeDisplayName` |
| 562 | `card.indicatorType.replace(/_/g, ' ')` → `card.indicatorTypeDisplayName` |
| 623 | `card.indicatorType.replace(/_/g, ' ')` → `card.indicatorTypeDisplayName` |

## 작업 리스트

- [x] `EnrichedFavoriteResponse.GlobalItem` 에 `indicatorTypeDisplayName` 필드 추가
- [x] `EnrichedFavoriteResponse.from` 4개 분기(failed/snap null/snap exists)에서 displayName 계산
- [x] `RecentUpdateResponse.GlobalUpdate` 에 `indicatorTypeDisplayName` 필드 추가
- [x] `RecentUpdateResponse.GlobalUpdate.from` 에서 `getDisplayName()` 매핑
- [x] `index.html:525` 최근 업데이트 — displayName 으로 교체
- [x] `index.html:562` GRAPH 카드 — displayName 으로 교체
- [x] `index.html:623` INDICATOR 카드 — displayName 으로 교체
- [ ] 강력 새로고침 후 한국어 노출 확인 (예: "물가상승률", "기준 금리")

## 비-목표

- 글로벌 페이지(`#global`) 자체 변경 없음
- enum 자체 변경 없음
- DTO `indicatorType` 영문 필드 제거/변경 없음 (키 호환 유지)
- 다른 언어(영어/일본어) i18n 도입 없음
- ECOS / 한국 지표 표시 변경 없음

## 주의사항

- 프론트 다른 곳에서 `indicatorType` 영문 필드를 라벨로 사용하는지 사전 확인 필요. (현재 영향 분석상 위 3곳뿐.)
- failed 카드(`INVALID_CODE`)는 enum 매핑 불가 → raw 문자열 fallback 그대로 노출.
