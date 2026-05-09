---
title: "Spring ApplicationRunner backfill — Postgres advisory lock + 부팅 차단 회피로 multi-replica 안전성 확보"
category: architecture-patterns
date: 2026-05-10
module: favorite, common-bootstrap
problem_type: best_practice
component: database
severity: high
tags:
  - spring-application-runner
  - bootstrap-backfill
  - multi-replica
  - postgres-advisory-lock
  - pg-try-advisory-xact-lock
  - boot-failure-isolation
  - idempotent-backfill
applies_when:
  - "ApplicationRunner로 부팅 시 1회 backfill을 실행해 nullable 신규 컬럼을 채울 때"
  - "rolling/blue-green 배포로 같은 시점에 여러 replica가 동시에 부팅될 때"
  - "ddl-auto + 수동 SQL 마이그레이션 하이브리드에서 phase 1~2 윈도우 동안 UNIQUE 제약 부재 상태로 backfill해야 할 때"
---

# Spring ApplicationRunner backfill — Postgres advisory lock + 부팅 차단 회피로 multi-replica 안전성 확보

## Context

본 PR은 `user_favorite_indicator`에 nullable `priority` 컬럼을 추가하고, 부팅 시 1회 ApplicationRunner로 NULL 행을 dense 0..N-1로 채우는 3-phase 마이그레이션을 채택했다. Phase 1(자바 배포 + backfill) ~ Phase 3(수동 SQL로 NOT NULL + UNIQUE DEFERRABLE 추가) 사이의 윈도우는 UNIQUE 제약 자체가 존재하지 않아, 다음 두 trap이 함께 발생할 수 있다:

1. **Multi-replica race** — rolling 배포로 두 replica가 동시에 부팅하면 둘 다 같은 NULL 행을 SELECT하고 동일 priority를 부여한다. UNIQUE가 없으니 둘 다 silent commit, phase 3 ALTER ADD CONSTRAINT가 중복 priority 발견으로 실패.
2. **부팅 차단** — `@Transactional` ApplicationRunner가 예외를 raise하면 Spring Boot의 `SpringApplication.run`이 `IllegalStateException`으로 감싸 부팅을 abort. 한 replica가 일시적 lock timeout/네트워크 문제로 실패하면 그 인스턴스는 unhealthy, 다른 replica가 정상이라도 LB가 절반의 트래픽을 거부한다.

본 패턴은 두 trap을 모두 차단한다.

## Guidance

### 1. `pg_try_advisory_xact_lock`으로 단일 replica에서만 backfill

Postgres advisory lock은 트랜잭션 단위로 `bigint` 키에 대해 mutex를 제공한다. 여러 replica가 같은 키로 동시에 시도하면 한 replica만 true 반환, 나머지는 false 반환 → 즉시 skip.

```java
private static final long ADVISORY_LOCK_KEY = 4242042001L;  // 본 backfill 작업 전용 상수

private boolean tryAcquireAdvisoryLock() {
    Object result = entityManager
        .createNativeQuery("SELECT pg_try_advisory_xact_lock(:key)")
        .setParameter("key", ADVISORY_LOCK_KEY)
        .getSingleResult();
    return Boolean.TRUE.equals(result);
}

@Transactional
protected void backfillWithLock() {
    if (!tryAcquireAdvisoryLock()) {
        log.info("FAVORITE_PRIORITY_BACKFILL_SKIPPED reason=advisory-lock-held-by-another-replica");
        return;
    }
    // ... NULL 행 조회 + dense 0..N-1 부여 + bulk update ...
}
```

핵심:
- **`pg_try_advisory_xact_lock`은 non-blocking** — 즉시 true/false 반환. 다른 replica가 lock 보유 시 대기 없이 skip해 부팅 시간 보장.
- **트랜잭션 단위 자동 해제** — `_xact_` 접미사가 transaction-scoped. commit/rollback 시 자동 해제. session-scoped(`pg_try_advisory_lock`)는 명시적 unlock 필요해 함정.
- **상수 키 충돌 방지** — 본 backfill 전용 키를 정해 다른 advisory lock(다른 모듈, 다른 backfill)과 충돌 회피. 키 영역 분리 정책을 코멘트로 명시.

### 2. try/catch로 부팅 차단 회피 + 다음 부팅 재시도

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class FavoritePriorityBackfillRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        try {
            backfillWithLock();
        } catch (Exception e) {
            log.error("FAVORITE_PRIORITY_BACKFILL_FAILED next-boot-will-retry", e);
        }
    }

    @Transactional
    protected void backfillWithLock() {
        // ... advisory lock + backfill 로직 ...
    }
}
```

- **`run()`에 try/catch** — 어떤 예외가 raise되어도 ERROR 로그만 남기고 부팅 자체는 진행. 실패한 replica가 unhealthy로 전체 배포가 멈추는 사고 회피.
- **`@Transactional`을 inner method에 두기** — `run()` 자체에 `@Transactional`을 두면 try/catch가 트랜잭션 인터셉터 *외부*에 위치해 commit 단계 예외(예: DEFERRED constraint check, ConstraintViolation at flush)가 catch에 도달하지 못할 수 있다. inner protected method에 `@Transactional`을 두면 Spring AOP 프록시가 그 경계에서 commit을 처리하고, 발생 예외를 호출자(`run()`)에서 정상 catch.
- **idempotent 보장이 전제** — 다음 부팅 재시도가 안전하려면 backfill 자체가 idempotent해야 한다. `WHERE priority IS NULL`로 입력을 한정하고 모두 채워지면 즉시 종료하는 패턴이 표준.

### 3. inner method 가시성 — `protected` + non-final

`@Transactional`을 inner method에 두려면 Spring AOP가 프록시를 만들 수 있어야 하므로 `private`은 안 됨(self-invocation은 프록시를 거치지 않음). `protected`(또는 package-private/public)이고 non-final이어야 한다. 본 repo의 다른 ApplicationRunner들도 동일 패턴.

### 4. 운영 가시성 — 명확한 로그 키워드

```text
FAVORITE_PRIORITY_BACKFILL_APPLIED rows=N         # 정상 적용
FAVORITE_PRIORITY_BACKFILL_SKIPPED reason=...     # 다른 replica가 처리 중 (정상)
FAVORITE_PRIORITY_BACKFILL_FAILED ...             # 실패 — 다음 부팅 재시도
```

각 키워드는 운영 모니터링(ELK/Loki)의 별도 alert 룰로 등록한다. APPLIED는 INFO trend 모니터, SKIPPED는 INFO 정상 신호, FAILED는 ERROR alert로 즉시 escalation.

## Why This Matters

- **Rolling 배포 안전성**: blue/green 또는 multi-replica 환경에서 backfill race로 데이터 무결성이 깨지면 phase 3 SQL이 실패하고 운영자가 수동 정리해야 한다. 본 패턴으로 race 자체를 차단.
- **부팅 가용성 보존**: 한 replica의 일시적 backfill 실패가 LB의 절반을 잘라먹지 않도록 보장. ApplicationRunner 예외가 부팅을 abort한다는 점을 모르고 try/catch를 빠뜨리면 이 trap이 발생.
- **3-phase 마이그레이션의 첫 phase 안전성**: 본 repo의 `jpa-version-passthrough-...` 패턴이 phase 1 entity-side 함정을 다룬다면, 본 문서는 phase 1 runtime-side(부팅 시 backfill 동작 안전성)를 다룬다 — 함께 적용해야 phase 1이 완성.

## When to Apply

- ApplicationRunner로 부팅 시 1회 backfill을 실행해야 할 때
- rolling/blue-green 배포로 같은 시점에 여러 replica가 동시에 부팅할 가능성이 있을 때
- backfill 도중 일시적 장애(lock timeout, 네트워크 일시 단절)에 대해 부팅 차단보다 next-boot 재시도가 합리적일 때
- ddl-auto + 수동 SQL 하이브리드 마이그레이션의 phase 1 ~ phase 3 윈도우 동안 UNIQUE 제약 부재 상태로 backfill해야 할 때

## Examples

### 함정 시나리오 — try/catch 누락 + advisory lock 누락

```java
// 잘못된 패턴
@Override
@Transactional
public void run(ApplicationArguments args) {
    List<Entity> nullRows = repo.findAllWithNullPriority();
    if (nullRows.isEmpty()) return;
    // 두 replica가 같은 NULL 행을 동시에 본다 → 같은 priority 부여 → silent 중복
    repo.assignDensePriority(nullRows);
}
```

- replica A와 B가 동시 부팅 시 둘 다 nullRows의 같은 행 리스트를 받음 → 같은 priority 부여 → phase 3 ALTER UNIQUE가 중복 발견으로 실패
- 만약 A가 lock timeout으로 실패하면 `@Transactional` rollback + 예외가 부팅을 abort → A는 unhealthy, LB의 절반이 트래픽을 거부

### 본 작업 적용 사례

- 변경 파일: `src/main/java/com/thlee/stock/market/stockmarket/favorite/infrastructure/config/FavoritePriorityBackfillRunner.java`
- advisory lock 키: `4242042001L` (favorite priority backfill 전용 상수)
- inner method: `protected void backfillWithLock()` — `@Transactional` + Spring AOP 프록시 호환
- 부팅 시 ERROR 발생해도 LB-down 회피 → 다음 부팅에서 idempotent 재시도

## Related

- `docs/solutions/architecture-patterns/jpa-version-passthrough-ddl-auto-not-null-backfill-2026-04-28.md` — 같은 3-phase 마이그레이션의 entity-side 함정 (NOT NULL silent skip)
- `docs/solutions/architecture-patterns/deferred-unique-constraint-retry-requires-new-2026-05-10.md` — 본 PR의 retry semantics (phase 3 이후의 동시성 패턴)
- `docs/plans/2026-05-07-001-feat-watchlist-priority-and-graph-layout-plan.md` — Issue #42 plan, Unit 1 (backfill runner) + Documentation/Operational Notes (롤백 시나리오)
- 외부 참고: PostgreSQL `pg_try_advisory_xact_lock` 문서, Spring `Propagation.REQUIRES_NEW` + AOP self-invocation 제약