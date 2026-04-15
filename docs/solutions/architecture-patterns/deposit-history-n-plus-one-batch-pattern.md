---
title: "N+1 Query in Portfolio Deposit History and Code Quality Issues"
category: performance-issues
date: 2026-04-14
tags: [n-plus-one, batch-query, code-review, deposit-history, bean-validation, testability]
modules: [portfolio]
severity: medium
symptoms: ["slow portfolio getItems", "multiple deposit history queries in loop", "duplicate maturity calculation", "untestable LocalDate.now usage"]
root_cause: "Per-item deposit history query inside loop causing N+1 problem, compounded by duplicated logic and missing validation"
---

# N+1 Query in Portfolio Deposit History and Code Quality Issues

## Problem

포트폴리오 납입 이력(DepositHistory) 기능 구현 후, `PortfolioService.getItems()`에서 CASH/FUND 항목별로 개별 `findByPortfolioItemId()` 쿼리를 실행하는 N+1 문제가 발견되었다. 코드 리뷰에서 추가로 만기 계산 로직 중복, no-op 코드, DTO 검증 누락, 테스트 불가능한 `LocalDate.now()` 사용 등이 발견되었다.

## Solution

### 1. N+1 → Batch Query (P1)

Repository 포트/JPA/어댑터에 `findByPortfolioItemIdIn` 배치 메서드 추가:

```java
// Port interface
List<DepositHistory> findByPortfolioItemIdIn(List<Long> portfolioItemIds);

// JPA Repository
List<DepositHistoryEntity> findByPortfolioItemIdInOrderByDepositDateAsc(List<Long> portfolioItemIds);
```

`getItems()`에서 N개 쿼리 → 1개 쿼리로 변경:

```java
// BEFORE: N queries
depositMap = depositTargetIds.stream()
    .collect(Collectors.toMap(id -> id, id -> depositHistoryRepository.findByPortfolioItemId(id)));

// AFTER: 1 query + in-memory grouping
depositMap = depositHistoryRepository.findByPortfolioItemIdIn(depositTargetIds)
    .stream().collect(Collectors.groupingBy(DepositHistory::getPortfolioItemId));
```

### 2. Duplicate Maturity Calculation (P2)

public `calculateExpectedMaturityAmount()`가 private `calculateMaturityAmount()`에 위임하도록 통합:

```java
public BigDecimal calculateExpectedMaturityAmount(Long userId, Long itemId) {
    PortfolioItem item = findUserItem(userId, itemId);
    if (item.getAssetType() != AssetType.CASH || item.getCashDetail() == null) return null;
    return calculateMaturityAmount(item); // 단일 소스
}
```

### 3. No-op Removal (P2)

빈 이력 시 자기 자신에게 값을 다시 쓰는 no-op 제거:

```java
// BEFORE
if (histories.isEmpty()) { item.updateAmount(item.getInvestedAmount()); return; }

// AFTER
if (histories.isEmpty()) { return; }
```

### 4. DTO Validation (P2)

`DepositRequest`에 Bean Validation 추가, Controller에 `@Valid` 적용:

```java
@NotNull(message = "납입 금액은 필수입니다.")
@DecimalMin(value = "0.01", message = "납입 금액은 0보다 커야 합니다.")
private BigDecimal amount;

@Size(max = 200, message = "메모는 200자 이내로 입력해주세요.")
private String memo;
```

### 5. Testable Date (P3)

`isDepositOverdue`에 `LocalDate referenceDate` 파라미터 추가:

```java
// BEFORE: untestable
public boolean isDepositOverdue(PortfolioItem item, List<DepositHistory> histories) {
    LocalDate today = LocalDate.now(); ...
}

// AFTER: testable
public boolean isDepositOverdue(PortfolioItem item, List<DepositHistory> histories, LocalDate referenceDate) {
    // referenceDate 사용, 테스트에서 고정 날짜 주입 가능
}
```

## Prevention

**N+1 쿼리 방지**
- 코드 리뷰 체크리스트: "루프 내부에서 단건 DB 조회를 호출하는가?" → 배치 조회로 전환
- 리스트의 연관 데이터 로딩 시 항상 `WHERE id IN (...)` 배치 쿼리 사용

**Request DTO 검증**
- 모든 Request DTO에 Bean Validation(`@NotNull`, `@DecimalMin`, `@Size` 등) 필수 적용
- Controller 파라미터에 `@Valid` 누락 여부 리뷰 시 확인

**테스트 가능성**
- 비즈니스 로직에서 `LocalDate.now()` 직접 호출 금지. 날짜 파라미터를 주입하여 테스트 시 고정 날짜 사용

## Related Documentation

- `docs/solutions/architecture-patterns/global-indicator-history-mirroring.md` — 대량 데이터 적재 시 배치 처리 및 ID 기반 조회 패턴
- `docs/solutions/architecture-patterns/ecos-timeseries-chart-visualization.md` — 데이터 조회/변환 패턴 참고
- `docs/plans/2026-04-10-002-feat-portfolio-deposit-history-plan.md` — 기능 설계 문서
