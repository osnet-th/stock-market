---
title: "Postgres DEFERRABLE INITIALLY DEFERRED + Spring @Transactional retry — REQUIRES_NEW로 격리해야 retry가 살아있다"
category: architecture-patterns
date: 2026-05-10
module: favorite, common-persistence
problem_type: best_practice
component: database
severity: high
tags:
  - postgres
  - deferrable-constraint
  - initially-deferred
  - sqlstate-23505
  - sqlstate-25p02
  - spring-transactional
  - propagation-requires-new
  - retry-pattern
applies_when:
  - "Postgres `UNIQUE ... DEFERRABLE INITIALLY DEFERRED` 제약을 가진 테이블에 INSERT/UPDATE 실패 시 retry를 시도할 때"
  - "Spring `@Transactional` 안에서 try-catch + 동일 statement 재실행 패턴을 도입할 때"
  - "Hibernate `ddl-auto=update` + 수동 SQL 마이그레이션 하이브리드 환경에서 DEFERRABLE 제약을 SQL로만 관리할 때"
---

# Postgres DEFERRABLE INITIALLY DEFERRED + Spring @Transactional retry — REQUIRES_NEW로 격리해야 retry가 살아있다

## Context

본 repo는 `user_favorite_indicator` 테이블에 사용자 지정 우선순위(`priority` INT) 컬럼을 도입하고, 동시 swap-update를 안전하게 처리하기 위해 Postgres `UNIQUE (user_id, source_type, priority) DEFERRABLE INITIALLY DEFERRED` 제약을 채택했다. 동시 INSERT race(같은 그룹의 `MAX(priority)+1`을 두 트랜잭션이 동시에 산출하는 상황)에 대비해 단일 `@Transactional` 안에서 SQLState 23505 발생 시 retry하는 코드를 작성했다.

```java
// 잘못된 패턴 — retry 루프가 사실상 dead code
@Transactional
public boolean toggle(Long userId, ...) {
    if (deleted > 0) return false;
    insertWithRetry(userId, ...);   // 같은 트랜잭션 안에서 retry 시도
    return true;
}

private void insertWithRetry(...) {
    for (int attempt = 1; attempt <= 3; attempt++) {
        try {
            repo.insertWithNextPriority(...);  // INSERT ... SELECT MAX(priority)+1
            return;
        } catch (DataIntegrityViolationException e) {
            if (!isUniqueViolation(e) || attempt == 3) throw e;
            Thread.sleep(50L * attempt);  // backoff
        }
    }
}
```

이 패턴은 **DEFERRABLE INITIALLY DEFERRED 제약의 검증 시점을 잘못 가정한 dead code**다. 1차 attempt 실패 시 트랜잭션이 rollback-only 상태로 마킹되어 후속 attempt가 25P02(`current transaction is aborted`)로 즉시 거부되며, 그 결과 23505는 단 한 번도 다시 raise되지 않는다.

## Guidance

### DEFERRABLE INITIALLY DEFERRED 제약은 **commit 시점에 검증된다**

- `INITIALLY IMMEDIATE`: INSERT/UPDATE statement 직후 검증 → catch 가능
- `INITIALLY DEFERRED`: 트랜잭션 commit 시점에 한 번만 검증 → 메서드 본문 내 catch 불가능

본 repo의 swap-update(reorder) 시나리오는 mid-transaction에 priority 정수가 일시적으로 중복되는 상태를 허용해야 하므로 DEFERRED가 필요. 그러나 INSERT race retry는 IMMEDIATE-style 동작이 필요해 두 요구가 충돌한다.

### 해결책 — INSERT만 별도 빈으로 분리해 `REQUIRES_NEW`로 격리

```java
@Service
@RequiredArgsConstructor
public class FavoritePriorityInserter {

    private final FavoriteIndicatorRepository repo;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insert(Long userId, FavoriteIndicatorSourceType sourceType, String code) {
        repo.insertWithNextPriority(userId, sourceType, code);
    }
}
```

```java
// 호출자 — outer @Transactional 안에서 별도 트랜잭션 호출
private void insertWithRetry(Long userId, ...) {
    for (int attempt = 1; attempt <= 3; attempt++) {
        try {
            favoritePriorityInserter.insert(userId, ...);   // REQUIRES_NEW
            return;
        } catch (DataIntegrityViolationException e) {
            if (!isUniqueViolation(e) || attempt == 3) throw e;
            sleepBackoff(attempt);
        }
    }
}
```

핵심:
1. **별도 빈으로 분리** — Spring AOP self-invocation 제약상 같은 클래스의 `@Transactional` 메서드 호출은 propagation이 무시된다. `FavoritePriorityInserter`처럼 별도 `@Service` 빈에 두면 프록시를 통해 호출되어 새 트랜잭션이 시작된다.
2. **각 attempt가 독립 트랜잭션** — INSERT 실패 시 inner 트랜잭션만 rollback되며 commit 시 23505가 raise. 호출자는 그 시점에 catch 가능. 다음 attempt는 새 트랜잭션을 시작하므로 25P02 상태 영향 없음.
3. **idempotent-ish operation에만 사용** — outer 트랜잭션과 분리해도 안전한 단일 statement(부수효과 없음, retry 시 의미상 같은 결과)에 한정.

### SQLState 정확 매칭으로 retry 대상 한정

```java
private boolean isUniqueViolation(DataIntegrityViolationException e) {
    Throwable cause = e.getCause();
    while (cause != null) {
        if (cause instanceof SQLException sqlEx && "23505".equals(sqlEx.getSQLState())) {
            return true;
        }
        cause = cause.getCause();
    }
    return false;
}
```

`DataIntegrityViolationException`은 23505 외에도 NOT NULL 위반, FK 위반 등 다른 무결성 위반을 포함한다. SQLState로 정확 매칭해 retry 대상을 transient race로 한정하지 않으면 영구 결함을 무한 retry로 가린다.

### swap-update(reorder)에는 DEFERRED 그대로 + 메서드 내 catch 제거

reorder처럼 mid-tx 중복 priority가 알고리즘 정합성에 필수인 경우 DEFERRED를 유지하고, 메서드 본문의 `DataIntegrityViolationException` catch는 제거한다(어차피 commit 시 raise되어 메서드 외부로 던져지므로 dead code). 글로벌 핸들러(`@RestControllerAdvice`)에서 23505를 409로 매핑해 클라이언트에 전달.

```java
@Transactional
public void reorder(...) {
    // ... computeNewOrder + bulkUpdatePriority + assertGroupInvariant
    // DEFERRED UNIQUE 위반은 commit 시점에 raise되므로 본 메서드 내 catch 불가 —
    // GlobalExceptionHandler.handleDataIntegrityViolation이 409 CONFLICT로 매핑.
}
```

## Why This Matters

- **Silent reliability degradation**: 잘못된 retry 패턴은 컴파일 통과 + 단위 테스트로 발견 어려움 + 동시성 race 상황에서만 노출. 사용자에게 "추가 실패" 5xx가 노출되어도 retry log가 정상으로 보여 원인 추적이 늦어진다.
- **자주 반복되는 함정**: Postgres DEFERRED + Spring `@Transactional`은 본 repo 외에도 흔한 조합. 본 패턴 미숙지 시 다른 모듈에서도 같은 dead retry를 도입할 가능성.
- **문서화 가치**: 본 repo의 `external-http-per-item-transaction-isolation-2026-04-26.md`가 self-invocation 제약을 다룬다 — 본 문서는 DEFERRED 제약 시점 차이를 다루는 동일한 family의 trap.

## When to Apply

- Postgres `DEFERRABLE INITIALLY DEFERRED` 제약을 가진 테이블에 INSERT race retry를 도입할 때
- Spring `@Transactional` 안에서 같은 statement를 try-catch로 재실행하려 할 때 (PSQLException 발생 후 재실행)
- Hibernate `ddl-auto=update` + 수동 SQL 마이그레이션 환경에서 DEFERRABLE 제약을 SQL로만 관리할 때 — Entity의 `@UniqueConstraint(columnNames=...)`를 사용하면 ddl-auto가 NOT DEFERRABLE 중복 제약을 silent하게 추가해 본 패턴 자체가 깨진다(Entity에 절대 두지 말 것)
- swap-update(bulk reorder)와 single-row INSERT가 같은 제약을 공유하지만 mid-tx 중복 허용 요구가 다를 때

## Examples

### 함정 시나리오 — 같은 트랜잭션 내 retry는 죽어있다

```text
T1: BEGIN
T1: INSERT ... priority = 5  -- 성공
T2: BEGIN
T2: INSERT ... priority = 5  -- 성공 (DEFERRED는 statement 시점에 검증 안 함)
T1: COMMIT  -- 성공
T2: COMMIT  -- 23505 raise (deferred check fires)

# 코드 흐름:
# 1. inner repo.insertWithNextPriority 정상 종료(예외 없음)
# 2. outer toggle() 메서드 본문 정상 종료
# 3. Spring TransactionInterceptor가 commit 시도
# 4. 여기서 23505 발생 — toggle() 내부의 catch는 이미 호출 스택에서 빠져나간 상태
# 5. catch 블록은 실행될 수 없음 — retry 루프는 dead code
```

### 올바른 패턴 — REQUIRES_NEW로 retry 격리

```text
T1: BEGIN [outer]
T1: BEGIN [inner — REQUIRES_NEW]
T1: INSERT ... priority = 5
T2: BEGIN [outer]
T2: BEGIN [inner — REQUIRES_NEW]
T2: INSERT ... priority = 5
T1: COMMIT [inner]  -- 성공
T2: COMMIT [inner]  -- 23505 raise here, propagation REQUIRES_NEW 덕에 inner만 rollback

# 코드 흐름:
# 1. T2의 favoritePriorityInserter.insert(...)가 23505로 throw
# 2. T2의 toggle() 내 try-catch가 catch — inner 트랜잭션은 이미 rollback됨, outer는 살아있음
# 3. attempt = 2로 retry
# 4. 새 inner 트랜잭션에서 INSERT ... SELECT MAX(priority)+1 = 6 → 성공
```

### 본 작업 적용 사례

- 변경 파일: `src/main/java/com/thlee/stock/market/stockmarket/favorite/application/FavoritePriorityInserter.java`(신규), `FavoriteIndicatorService.java`(insertWithRetry/reorder catch 정정)
- 마이그레이션 SQL: `src/main/resources/db/migration/user_favorite_indicator_priority_not_null_unique_deferrable.sql` — 단일 트랜잭션(`BEGIN; ... COMMIT;`)으로 적용, Entity에 `@UniqueConstraint` 두지 말 것 명시
- 운영 모니터링: `FAVORITE_INSERT_UNIQUE_RETRY_EXHAUSTED` ERROR 로그 — 5분 내 3회 이상 발생 시 escalation

## Related

- `docs/solutions/architecture-patterns/external-http-per-item-transaction-isolation-2026-04-26.md` — Spring AOP self-invocation 제약과 per-item 격리 패턴 (본 패턴의 형제 trap)
- `docs/solutions/architecture-patterns/jpa-version-passthrough-ddl-auto-not-null-backfill-2026-04-28.md` — `ddl-auto=update` + NOT NULL 컬럼 추가의 silent skip 함정 (본 마이그레이션의 phase 1에서 동일 위험 회피)
- `docs/plans/2026-05-07-001-feat-watchlist-priority-and-graph-layout-plan.md` — Issue #42 plan, Unit 1.5 + Unit 4 의 race 모델
- 외부 참고: PostgreSQL `SET CONSTRAINTS` 문서, Christian Emmer "Deferrable Constraints in PostgreSQL"