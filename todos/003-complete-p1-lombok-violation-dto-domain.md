---
status: complete
priority: p1
issue_id: 003
tags: [code-review, code-style, lombok, claude-md-rule, portfolio]
dependencies: []
---

# CLAUDE.md 규칙 11 위반 — Lombok 미사용 수동 생성자/getter (StockSaleHistoryResponse + StockSaleHistory)

## Problem Statement

CLAUDE.md 규칙 11("getter/setter는 Lombok 애노테이션 사용, 수동 작성 금지")을 위반한 두 클래스.

## Findings

### 위치 1: `application/dto/StockSaleHistoryResponse.java:14-92`
- 21개 필드의 private 생성자 + 모든 getter를 수기 작성 (~60 LOC 잉여)
- 옆에 있는 `StockSaleContextResponse`는 `@RequiredArgsConstructor` 사용 → 일관성 깨짐

### 위치 2: `domain/model/StockSaleHistory.java:42-89`
- 21-인자 재구성용 생성자 수동 작성

## Proposed Solutions

### Option A — `@Getter` + `@RequiredArgsConstructor` 적용
```java
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StockSaleHistoryResponse {
    private final Long id;
    ...
    public static StockSaleHistoryResponse from(StockSaleHistory h) { ... }
}
```

### Option B — `@AllArgsConstructor(access=PRIVATE)` + 정적 팩토리만 노출
- 도메인 `StockSaleHistory`에 적합

## Recommended Action

A 또는 B 적용. `@JsonInclude(NON_NULL)` 함께 추가하면 별건 todo 018과 통합 처리 가능.

## Technical Details

- 영향 파일:
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/dto/StockSaleHistoryResponse.java`
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/domain/model/StockSaleHistory.java`

## Acceptance Criteria

- [ ] 두 파일 모두 수동 getter/생성자 제거, Lombok 어노테이션으로 대체
- [ ] 컴파일 통과
- [ ] 기존 호출처 시그니처 호환 유지

## Work Log

- 2026-04-27: ce-review 발견 (code-simplicity-reviewer P1)
- 2026-04-27: 적용 완료
  - `StockSaleHistoryResponse`: `@Getter` + `@RequiredArgsConstructor(access = PRIVATE)` + `@JsonInclude(NON_NULL)` (todo 016 통합 처리)
  - `StockSaleHistory` 도메인: `@AllArgsConstructor(access = PUBLIC)` 적용, 21-인자 수동 생성자 제거
  - 약 80 LOC 감소, 컴파일/테스트 통과