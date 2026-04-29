---
status: pending
priority: p2
issue_id: 040
tags: [code-review, architecture, newsjournal]
dependencies: [034]
---

# `NewsEventReadService` 가 `NewsEventCategoryRepository` 를 직접 호출 — `NewsEventCategoryService` 위임으로 단일화

## Problem Statement

`NewsEventReadService.findList` 와 `NewsEventCategoryService.findByUserId` 가 동일한 `findByUserIdOrderByNameAsc(userId)` 를 각자 호출. 두 진입점이 같은 데이터를 읽음 → 정책 변경 (캐싱, 정렬, 가시성 필터) 시 이중 수정 위험. 레이어 위반은 아니나 카테고리 도메인의 단일 진입점 일관성이 깨짐.

## Findings

- 위치 1: `src/main/java/.../newsjournal/application/NewsEventReadService.java:38, 62-65`
- 위치 2: `src/main/java/.../newsjournal/application/NewsEventCategoryService.java:25-28`

```java
// NewsEventReadService
for (NewsEventCategory c : categoryRepository.findByUserIdOrderByNameAsc(userId)) { ... }

// NewsEventCategoryService — 동일 호출
return categoryRepository.findByUserIdOrderByNameAsc(userId);
```

확인 보고:
- architecture-strategist P2 (#3)

## Proposed Solutions

### Option A — `NewsEventCategoryService` 위임 (권장)
- `NewsEventReadService` 의존성: `NewsEventCategoryRepository` → `NewsEventCategoryService`.
- 단건 조회는 `NewsEventCategoryService.findByIdAndUserId(id, userId)` 메서드 추가 또는 포트 직접 유지 (YAGNI).
- 장점: 카테고리 도메인 단일 진입점, 정책 일관성.
- 단점: Service 메서드 1개 추가.

### Option B — 034 와 함께 처리
- 034 (페이지 한정 IN-batch 조회) 적용 시 호출 패턴 자체가 바뀌므로 함께 검토.
- 장점: 한 번에 정리.

### Option C — 현행 유지
- ReadService 는 카테고리 데이터만 필요하지 use-case 가 아니므로 포트 직접 OK 라는 입장.
- 장점: 변경 없음.

## Recommended Action

(034 와 함께 결정)

## Technical Details

- 영향 파일: `NewsEventReadService.java`, (Option A) `NewsEventCategoryService.java`

## Acceptance Criteria

- [ ] 카테고리 조회 진입점 일관
- [ ] 컴파일 + 회귀 OK

## Work Log

- 2026-04-29: ce-review 발견 (architecture P2)

## Resources

- `src/main/java/.../newsjournal/application/NewsEventReadService.java`
- `src/main/java/.../newsjournal/application/NewsEventCategoryService.java`