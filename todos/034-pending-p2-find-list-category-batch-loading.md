---
status: pending
priority: p2
issue_id: 034
tags: [code-review, performance, n-plus-one, newsjournal]
dependencies: []
---

# `NewsEventReadService.findList` 의 카테고리 전수 로딩 — 페이지 한정 IN-batch 로 변경

## Problem Statement

`findList` 가 페이지(20건) 응답을 위해 사용자의 *전체* 카테고리를 in-memory map 으로 적재. 사용자 카테고리가 누적되면 페이지당 무관한 카테고리 객체를 매번 PG → JDBC → JPA → Domain 매핑하여 GC 압박 + 응답 지연 발생.

## Findings

- 위치: `src/main/java/.../newsjournal/application/NewsEventReadService.java:62-65`

```java
Map<Long, NewsEventCategory> categoryById = new HashMap<>();
for (NewsEventCategory c : categoryRepository.findByUserIdOrderByNameAsc(userId)) {
    categoryById.put(c.getId(), c);
}
```

영향 추정:
- 1인 + 카테고리 수십 개: 무시 가능 (수 KB, < 1ms).
- 카테고리 1,000개 누적 (자유 입력 + 자동 등록): 페이지당 무관한 ~995개 객체 매핑. 페이지 RTT 1회 + ~5–20ms 추가 + GC 압박. 무한 스크롤/필터 변경 시마다 반복.

확인 보고:
- performance-oracle P2 (#1)
- security-sentinel P2 (카테고리 폭증 시 메모리 부담)

## Proposed Solutions

### Option A — 페이지의 distinct categoryId 만 IN-batch 조회 (권장)
- `NewsEventCategoryRepository` 포트에 `findByUserIdAndIdIn(Long userId, Collection<Long> ids)` 추가.
- `findList` 본문:
```java
Set<Long> categoryIds = events.stream()
    .map(NewsEvent::getCategoryId).filter(Objects::nonNull)
    .collect(Collectors.toSet());
Map<Long, NewsEventCategory> categoryById = categoryIds.isEmpty()
    ? Map.of()
    : categoryRepository.findByUserIdAndIdIn(userId, categoryIds).stream()
        .collect(Collectors.toMap(NewsEventCategory::getId, c -> c));
```
- 장점: 페이지 size 에 비례, 카테고리 폭증 무관.
- 단점: 포트 메서드 1개 추가.

### Option B — 현행 유지 + 카테고리 개수 상한 강제
- 036 (카테고리 상한) 적용 시 무효 부담 제한.
- 장점: 코드 변경 없음.
- 단점: 상한 정책 결정 의존.

## Recommended Action

(triage 후 결정)

## Technical Details

- 영향 파일: `NewsEventReadService.java`, `NewsEventCategoryRepository.java`, `NewsEventCategoryJpaRepository.java`, `NewsEventCategoryRepositoryImpl.java`

## Acceptance Criteria

- [ ] 페이지당 카테고리 조회가 page size 에 비례
- [ ] 컴파일 + 응답 회귀 OK

## Work Log

- 2026-04-29: ce-review 발견 (performance P2)

## Resources

- `src/main/java/.../newsjournal/application/NewsEventReadService.java`
- `src/main/java/.../newsjournal/infrastructure/persistence/NewsEventCategoryJpaRepository.java`