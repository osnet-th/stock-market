---
status: pending
priority: p2
issue_id: 038
tags: [code-review, security, error-handling, newsjournal]
dependencies: []
---

# `NewsJournalExceptionHandler` 의 `assignableTypes` 가 `NewsEventCategoryController` 누락 — 401 가 500 으로 떨어짐

## Problem Statement

`NewsJournalExceptionHandler` 는 `@RestControllerAdvice(assignableTypes = NewsJournalController.class)` 로 한정되어 있어 신규 `NewsEventCategoryController` 는 커버하지 않음. 카테고리 컨트롤러의 `NewsJournalSecurityContext.currentUserId()` 가 던지는 `InsufficientAuthenticationException` 이 `GlobalExceptionHandler` 의 `Exception` 폴백으로 떨어져 **500 INTERNAL_ERROR** 로 응답 (401 가 아님). 응답 일관성 + 외부 에이전트의 표준 복구 (토큰 갱신) 불가.

## Findings

- 위치 1: `src/main/java/.../newsjournal/presentation/NewsJournalExceptionHandler.java:27`
- 위치 2: `src/main/java/.../newsjournal/presentation/NewsEventCategoryController.java:28`

```java
@RestControllerAdvice(assignableTypes = NewsJournalController.class)
public class NewsJournalExceptionHandler { ... }
```

확인 보고:
- security-sentinel P3 (#4)
- agent-native-reviewer P2 (#2)

## Proposed Solutions

### Option A — `assignableTypes` 확장 (권장, 1줄 수정)
```java
@RestControllerAdvice(assignableTypes = {
    NewsJournalController.class,
    NewsEventCategoryController.class
})
```

### Option B — `basePackages` 로 변경
```java
@RestControllerAdvice(basePackages = "com.thlee.stock.market.stockmarket.newsjournal.presentation")
```
- 장점: 미래 신규 컨트롤러 자동 커버.
- 단점: 같은 패키지의 다른 의도 컨트롤러도 흡수.

## Recommended Action

(Option A 권장 — 명시적 +향후 확장 시 명시 추가)

## Technical Details

- 영향 파일: `NewsJournalExceptionHandler.java`

## Acceptance Criteria

- [ ] 카테고리 컨트롤러의 401 가 401 로 응답
- [ ] 응답 shape 가 사건 컨트롤러와 동일

## Work Log

- 2026-04-29: ce-review 발견 (security P3, agent-native P2)

## Resources

- `src/main/java/.../newsjournal/presentation/NewsJournalExceptionHandler.java`
- `src/main/java/.../newsjournal/presentation/NewsEventCategoryController.java`