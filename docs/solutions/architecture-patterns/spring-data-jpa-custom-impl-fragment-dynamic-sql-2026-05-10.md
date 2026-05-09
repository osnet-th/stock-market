---
title: "Spring Data JPA — 가변 길이 dynamic SQL을 위한 custom-impl fragment 패턴"
category: architecture-patterns
date: 2026-05-10
module: favorite, common-persistence
problem_type: best_practice
component: database
severity: medium
tags:
  - spring-data-jpa
  - custom-impl-fragment
  - dynamic-sql
  - case-when-bulk-update
  - entity-manager
  - chunked-update
  - flush-clear
applies_when:
  - "JPA Repository에 가변 길이 입력(Map/List)에 의존하는 동적 SQL이 필요할 때"
  - "@Modifying @Query 정적 JPQL로는 표현 불가능한 CASE WHEN 일괄 UPDATE를 발행해야 할 때"
  - "Spring Data JPA가 자동 합쳐주는 fragment 패턴을 본 repo에 처음 도입할 때"
---

# Spring Data JPA — 가변 길이 dynamic SQL을 위한 custom-impl fragment 패턴

## Context

`@Modifying @Query`로 정의된 JPA Repository 메서드는 *static* JPQL/native SQL만 받는다. 다음 같은 가변 길이 입력에 의존하는 SQL은 표현 불가:

```sql
UPDATE user_favorite_indicator
SET priority = CASE id
    WHEN ?1 THEN ?2
    WHEN ?3 THEN ?4
    -- ... id가 N개면 N개의 WHEN
END
WHERE id IN (?N+1, ?N+2, ...);
```

본 PR의 reorder 알고리즘은 `Map<Long, Integer> idToPriority`를 받아 한 트랜잭션에서 일괄 priority 갱신해야 한다. 입력 크기가 가변이라 `@Query`로는 못 적는다 — Spring Data JPA의 **custom-impl fragment** 패턴을 본 repo에 처음 도입했다.

## Guidance

### 1. `*Custom` 인터페이스 + `*Impl` 클래스 + main repository 상속

Spring Data JPA는 main Repository 인터페이스가 `*Custom` 인터페이스를 추가로 상속하면, **`<RepositoryName>Impl` 클래스명을 자동 인식**해 custom 메서드를 main repository에 합쳐준다. 별도 `@Bean` 등록 불필요.

```java
// 1) Custom fragment 인터페이스 (도메인 의도만 노출)
public interface UserFavoriteIndicatorJpaRepositoryCustom {
    int bulkUpdatePriority(Map<Long, Integer> idToPriority);
}

// 2) 동적 SQL 구현체 — 클래스명이 정확히 *Impl 이어야 자동 인식됨
public class UserFavoriteIndicatorJpaRepositoryImpl
        implements UserFavoriteIndicatorJpaRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public int bulkUpdatePriority(Map<Long, Integer> idToPriority) {
        // ... 동적 SQL 빌드 + setParameter ...
    }
}

// 3) main Repository — 일반 JpaRepository + custom fragment를 동시 상속
public interface UserFavoriteIndicatorJpaRepository
        extends JpaRepository<UserFavoriteIndicatorEntity, Long>,
                UserFavoriteIndicatorJpaRepositoryCustom {

    // 일반 derived query / @Query
    List<UserFavoriteIndicatorEntity> findByUserId(Long userId);

    // custom 메서드는 자동으로 추가됨 — 별도 선언 불필요
}
```

호출 측은 `userFavoriteIndicatorJpaRepository.bulkUpdatePriority(map)` 형태로 일관되게 사용. main과 custom이 자연스럽게 합쳐진 단일 인터페이스로 보인다.

### 2. EntityManager 직접 사용 — chunk 분할 + flush/clear 책임

```java
private static final int SMALL_BATCH_THRESHOLD = 5;
private static final int CHUNK_SIZE = 50;

@Override
public int bulkUpdatePriority(Map<Long, Integer> idToPriority) {
    if (idToPriority == null || idToPriority.isEmpty()) {
        return 0;
    }
    int affected;
    if (idToPriority.size() <= SMALL_BATCH_THRESHOLD) {
        affected = updateOneByOne(idToPriority);
    } else {
        affected = updateInChunks(idToPriority);
    }
    // L1 cache가 stale entity를 반환하지 않도록 flush + clear.
    // 후속 read(예: post-write invariant assertion)가 mutated state를 정확히 본다.
    entityManager.flush();
    entityManager.clear();
    return affected;
}
```

핵심 책임:
- **chunk 분할**: CASE WHEN expression이 비대해지지 않도록 50개 단위로 분할. 작은 입력(≤5)은 개별 UPDATE로 단순화 — CASE WHEN 빌드 비용 회피.
- **flush + clear**: L1 cache는 dirty 추적용 + lazy 로딩용. 본 메서드는 native SQL로 직접 UPDATE하므로 cache는 갱신을 모른다. flush로 보류 중인 다른 변경을 먼저 반영 + clear로 stale entity 캐시 폐기 → 후속 read가 정확.
- **반환 row count**: chunk별 `executeUpdate()` 합산. 호출자가 변경 행 수로 no-op short-circuit 결정.

### 3. CASE WHEN 동적 빌드 — 파라미터 인덱스 정확성

```java
private int executeChunk(List<Map.Entry<Long, Integer>> chunk) {
    StringBuilder sql = new StringBuilder("UPDATE user_favorite_indicator SET priority = CASE id");
    for (int i = 0; i < chunk.size(); i++) {
        sql.append(" WHEN ?").append(i * 2 + 1).append(" THEN ?").append(i * 2 + 2);
    }
    sql.append(" END WHERE id IN (");
    for (int i = 0; i < chunk.size(); i++) {
        if (i > 0) sql.append(", ");
        sql.append("?").append(chunk.size() * 2 + i + 1);
    }
    sql.append(")");

    Query q = entityManager.createNativeQuery(sql.toString());
    for (int i = 0; i < chunk.size(); i++) {
        q.setParameter(i * 2 + 1, chunk.get(i).getKey());
        q.setParameter(i * 2 + 2, chunk.get(i).getValue());
    }
    for (int i = 0; i < chunk.size(); i++) {
        q.setParameter(chunk.size() * 2 + i + 1, chunk.get(i).getKey());
    }
    return q.executeUpdate();
}
```

파라미터 인덱스 약속:
- `1 .. 2N`: CASE WHEN의 (id, priority) 쌍. 1-기반 인덱스라 `i * 2 + 1`(WHEN id), `i * 2 + 2`(THEN priority).
- `2N+1 .. 3N`: WHERE IN 절 id. `chunk.size() * 2 + i + 1`.
- `setParameter(int, Object)`는 1-기반이며 모든 `?N` 자리에 값을 채워야 한다 — 누락 시 IllegalArgumentException.

빌드와 바인딩 양쪽이 같은 산식을 쓰므로 향후 리팩터링 시 한 곳만 변경하면 다른 곳도 동기화 필요. helper로 더 분해하면 안전하지만(예: `buildCaseWhenSql(chunk)` / `bindCaseParameters(q, chunk)`) 단일 메서드에 묶으면 산식 일관성이 시각적으로 즉시 확인됨.

### 4. native SQL 사용 — JPQL CASE WHEN의 한계

JPA spec의 JPQL은 `CASE id WHEN ... THEN ... END`를 제한적으로 지원하지만, Hibernate에서 기준 entity 타입 외 가변 표현을 받기 어렵다. `nativeQuery=true` + `createNativeQuery`가 가장 견고. 본 repo의 `NewsJpaRepository.java`도 native `INSERT ... ON CONFLICT` 패턴을 사용해 동일 정책 — 가변/dialect-specific SQL은 native로.

### 5. SQL injection 안전성

본 패턴의 모든 값은 `setParameter`로 바인딩되며, 동적 부분은 `?N` placeholder 자리뿐 — 사용자 입력 문자열을 SQL에 concat하지 않는다. 따라서 SQL injection 표면 없음. 동적 placeholder 개수는 `chunk.size()`에만 의존(서버 측 결정).

## Why This Matters

- **JPA `@Query`의 표현력 한계**: 가변 길이 입력에 의존하는 bulk DML은 `@Query`로 못 적는다 — 모르고 시도하면 컴파일은 통과하지만 런타임에 `NamedParameterNotBoundException` 또는 SQL syntax error로 터진다.
- **N+1 회피**: 행 단위 individual UPDATE를 루프하면 N번 round-trip + L1 cache 누적 → 대규모 입력에서 부팅/응답 시간 비대화. chunk CASE WHEN으로 N/50 round-trip으로 감축.
- **stale L1 cache 회피**: native UPDATE는 L1 cache를 갱신하지 않으므로 후속 read가 stale을 본다. flush + clear는 fragment 책임으로 두는 게 호출자 쪽에서 잊지 않는다.

## When to Apply

- 가변 길이 Map/List를 받아 한 트랜잭션에서 일괄 UPDATE/DELETE해야 할 때
- `@Modifying @Query` 정적 JPQL로 표현 불가능한 dynamic CASE WHEN, dynamic IN, dynamic ORDER BY가 필요할 때
- bulk DML 후 같은 트랜잭션 내에서 mutated row를 다시 read해야 할 때 (post-write invariant 검증, 일관성 점검 등)
- 본 repo에 동일 패턴(custom-impl fragment)이 다른 모듈에서도 필요해질 때 — 본 문서를 reference로 사용

## Examples

### 잘못된 시도 — `@Query`로 가변 CASE WHEN 표현 시도

```java
// 컴파일 통과 + 단위 테스트 인지 어려움 + 런타임에 실패
@Modifying
@Query("UPDATE UserFavoriteIndicatorEntity e SET e.priority = " +
       "CASE e.id WHEN ... END WHERE e.id IN (:ids)")
int bulkUpdatePriority(@Param("ids") List<Long> ids,
                       @Param("priorities") List<Integer> priorities);
```

`CASE e.id WHEN ... END`의 `...` 부분이 가변이라 정적 JPQL로 못 적음. 호출 시 NamedParameter 바인딩 실패.

### 본 작업 적용 사례

- 변경 파일:
  - `src/main/java/com/thlee/stock/market/stockmarket/favorite/infrastructure/persistence/UserFavoriteIndicatorJpaRepositoryCustom.java`(신규 fragment 인터페이스)
  - `UserFavoriteIndicatorJpaRepositoryImpl.java`(신규, EntityManager 직접 사용)
  - `UserFavoriteIndicatorJpaRepository.java`(`extends ..., UserFavoriteIndicatorJpaRepositoryCustom` 추가)
- 호출처:
  - `FavoriteIndicatorRepositoryImpl.bulkUpdatePriority` → 도메인 port에서 fragment 호출
  - `FavoritePriorityBackfillRunner.applyAssignments` → 동일 fragment 재사용으로 N+1 round-trip 회피
- chunk 정책: `CHUNK_SIZE = 50`, `SMALL_BATCH_THRESHOLD = 5`. 본 repo의 사용자 단위 favorites 항목은 30개 이내가 주류라 chunk 1회로 처리되며 small-batch도 흔함

## Related

- `docs/solutions/architecture-patterns/deposit-history-n-plus-one-batch-pattern.md` — 본 repo의 다른 bulk 패턴(`WHERE IN (...) + groupingBy`)과 보완 관계
- `docs/solutions/architecture-patterns/external-http-per-item-transaction-isolation-2026-04-26.md` — per-item 격리와 bulk 처리의 trade-off
- `docs/solutions/architecture-patterns/deferred-unique-constraint-retry-requires-new-2026-05-10.md` — 같은 PR의 DEFERRABLE UNIQUE 제약과 본 fragment의 swap-update 호환성
- 외부 참고: Spring Data JPA Reference — Custom Implementations for Spring Data Repositories (`*Custom` + `*Impl` 자동 인식 규약)
