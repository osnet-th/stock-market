---
status: pending
priority: p2
issue_id: 027
tags: [code-review, simplicity, newsjournal]
dependencies: []
---

# `NewsEventListItemResult` 와 `NewsEventDetailResult` 구조 100% 동일 — 통합

## Problem Statement

두 record 가 `(NewsEvent event, List<NewsEventLink> links)` 로 구조 동일. 의미 차이(상세 vs 리스트 항목)는 주석 수준이며, 실제 사용처에서 동일한 매핑 로직이 반복된다. 단일 record 로 통합하면 application/dto 1개 파일 + 매핑 1개 메서드 절감.

## Findings

- `src/main/java/com/thlee/stock/market/stockmarket/newsjournal/application/dto/NewsEventDetailResult.java`
- `src/main/java/com/thlee/stock/market/stockmarket/newsjournal/application/dto/NewsEventListItemResult.java`
- presentation 매핑: `NewsEventDetailResponse.from(...)`, `NewsEventListResponse.ItemDto.from(...)`

## Proposed Solutions

### Option A — `NewsEventWithLinks` 단일 record 로 통합 (Recommended)
- 두 파일 → 1개 (`NewsEventWithLinks.java`).
- `NewsEventReadService.findById` 반환 타입 = `NewsEventWithLinks`.
- `NewsEventListResult.items` = `List<NewsEventWithLinks>`.
- presentation Response 두 곳의 `from(...)` 시그니처는 동일 record 를 받음. 평탄화 로직은 그대로.

### Option B — 현 상태 유지 (의미 분리 가치 보존)
- 미래에 List 와 Detail 의 필드가 갈라질 가능성 대비. 다만 현재 동일하면 YAGNI 위반.

## Recommended Action

A 적용. YAGNI 원칙에 부합.

## Technical Details

- 변경 파일: 2 → 1 (rename). ReadService, WriteService(영향 없음 추정), presentation Response 매핑 시그니처.
- 컴파일 단위 테스트 회귀 없음 (record 구조 동일).

## Acceptance Criteria

- [ ] `NewsEventListItemResult` / `NewsEventDetailResult` 제거 또는 통합
- [ ] ReadService 반환 타입 갱신, 컴파일 통과
- [ ] presentation Response `from(...)` 시그니처 갱신

## Work Log

- 2026-04-28: 발견 (ce-review 단순성 #1)

## Resources

- code-simplicity-reviewer #1