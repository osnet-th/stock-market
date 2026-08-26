---
title: "Spring ApplicationRunner backfill — 별도 빈 분리로 @Transactional + advisory lock을 정확히 적용하기"
category: architecture-patterns
date: 2026-05-10
last_updated: 2026-05-10
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
  - spring-aop-self-invocation
  - executor-bean-isolation
applies_when:
  - "ApplicationRunner로 부팅 시 1회 backfill을 실행해 nullable 신규 컬럼을 채울 때"
  - "rolling/blue-green 배포로 같은 시점에 여러 replica가 동시에 부팅될 때"
  - "ddl-auto + 수동 SQL 마이그레이션 하이브리드에서 phase 1~2 윈도우 동안 UNIQUE 제약 부재 상태로 backfill해야 할 때"
---

# Spring ApplicationRunner backfill — 별도 빈 분리로 @Transactional + advisory lock을 정확히 적용하기

> **본 문서는 2026-05-10 운영 incident 후 재작성됨.** 초기 작성본은 `protected` inner method에 `@Transactional`을 두는 패턴을 권장했으나, Spring AOP self-invocation 제약 + Spring `@Transactional` 기본 정책상 protected method 미advise로 인해 운영에서 backfill이 silent하게 실패하고 NULL priority 행이 그대로 남는 사고가 발생함. 본 문서는 별도 `Executor` 빈으로 분리하는 정답 패턴으로 교체됨.

## Context

본 PR(#42)은 `user_favorite_indicator`에 nullable `priority` 컬럼을 추가하고, 부팅 시 1회 ApplicationRunner로 NULL 행을 dense 0..N-1로 채우는 3-phase 마이그레이션을 채택했다. Phase 1(자바 배포 + backfill) ~ Phase 3(수동 SQL로 NOT NULL + UNIQUE DEFERRABLE 추가) 사이의 윈도우는 UNIQUE 제약 자체가 존재하지 않아, 다음 두 trap이 함께 발생할 수 있다:

1. **Multi-replica race** — rolling 배포로 두 replica가 동시에 부팅하면 둘 다 같은 NULL 행을 SELECT하고 동일 priority를 부여한다. UNIQUE가 없으니 둘 다 silent commit, phase 3 ALTER ADD CONSTRAINT가 중복 priority 발견으로 실패.
2. **부팅 차단** — `@Transactional` ApplicationRunner가 예외를 raise하면 Spring Boot의 `SpringApplication.run`이 `IllegalStateException`으로 감싸 부팅을 abort. 한 replica가 일시적 lock timeout/네트워크 문제로 실패하면 그 인스턴스는 unhealthy, 다른 replica가 정상이라도 LB가 절반의 트래픽을 거부한다.

본 패턴은 두 trap을 모두 차단하면서, 동시에 **Spring AOP self-invocation 함정**을 회피한다.

## Guidance

### 1. 별도 `Executor` 빈으로 backfill 로직을 분리한다 (정답 패턴)

ApplicationRunner의 `run()` 안에서 같은 클래스의 `@Transactional` 메서드를 호출하면 Spring AOP가 우회되어 `@Transactional`이 무시된다. 또한 Spring `@Transactional` 기본 정책은 **public 메서드만 advise**하므로 protected/private는 환경에 따라 silent fail한다. 두 함정을 동시에 회피하는 정답 패턴은 **별도 `@Service` 빈으로 분리**해 외부 호출자가 프록시를 거치도록 보장하는 것:

```java
// (1) Backfill 로직을 별도 빈에 격리 — 외부 호출 시 Spring AOP 프록시 통과 보장
@Slf4j
@Component
@RequiredArgsConstructor
public class FavoritePriorityBackfillExecutor {

    private static final long ADVISORY_LOCK_KEY = 4242042001L;

    private final UserFavoriteIndicatorJpaRepository repository;
    private final EntityManager entityManager;

    @Transactional   // public + 외부 빈 호출 → @Transactional이 정확히 적용됨
    public void run() {
        if (!tryAcquireAdvisoryLock()) {
            log.info("FAVORITE_PRIORITY_BACKFILL_SKIPPED reason=advisory-lock-held-by-another-replica");
            return;
        }
        // ... NULL 행 조회 + dense 0..N-1 부여 + bulk update ...
        log.info("FAVORITE_PRIORITY_BACKFILL_APPLIED rows={}", n);
    }

    private boolean tryAcquireAdvisoryLock() {
        Object result = entityManager
            .createNativeQuery("SELECT pg_try_advisory_xact_lock(:key)")
            .setParameter("key", ADVISORY_LOCK_KEY)
            .getSingleResult();
        return Boolean.TRUE.equals(result);
    }
}

// (2) Runner는 thin wrapper — 외부 빈 호출이라 프록시 통과
@Slf4j
@Component
@RequiredArgsConstructor
public class FavoritePriorityBackfillRunner implements ApplicationRunner {

    private final FavoritePriorityBackfillExecutor executor;

    @Override
    public void run(ApplicationArguments args) {
        try {
            executor.run();   // 외부 빈 호출 → @Transactional 정확히 적용됨
        } catch (Exception e) {
            log.error("FAVORITE_PRIORITY_BACKFILL_FAILED next-boot-will-retry", e);
        }
    }
}
```

**왜 이 분리가 필요한가**:
- Spring AOP는 같은 클래스의 self-invocation을 프록시 객체를 통하지 않는 직접 호출로 처리해 `@Transactional`을 우회한다 — 이는 잘 알려진 함정이며 Spring 공식 문서에도 명시
- 같은 클래스 내 `protected backfillWithLock()` 호출은 Spring AOP 프록시가 그 경계에서 commit을 처리한다는 통념과 달리 실제로는 advise되지 않음
- 별도 `@Service`/`@Component` 빈에 두면 호출 시 Spring DI 컨테이너가 주입한 *프록시*를 거치므로 `@Transactional`이 정확히 적용됨
- public 메서드를 사용해 Spring `@Transactional` 기본 advise 정책과 align

본 repo의 `FavoritePriorityInserter` 빈도 동일 패턴 — 같은 PR의 `deferred-unique-constraint-retry-requires-new-2026-05-10.md` 문서가 REQUIRES_NEW 격리에 같은 분리를 가르친다.

### 2. `pg_try_advisory_xact_lock`으로 단일 replica에서만 backfill

Postgres advisory lock은 트랜잭션 단위로 `bigint` 키에 대해 mutex를 제공한다. 여러 replica가 같은 키로 동시에 시도하면 한 replica만 true 반환, 나머지는 false 반환 → 즉시 skip.

```java
private boolean tryAcquireAdvisoryLock() {
    Object result = entityManager
        .createNativeQuery("SELECT pg_try_advisory_xact_lock(:key)")
        .setParameter("key", ADVISORY_LOCK_KEY)
        .getSingleResult();
    return Boolean.TRUE.equals(result);
}
```

핵심:
- **반드시 `@Transactional` 메서드 안에서 호출** — `_xact_` 접미사가 transaction-scoped라 트랜잭션 외부에서 호출 시 lock이 즉시 해제되어 무의미. 본 호출이 자동으로 짧은 트랜잭션을 만드는 환경(JPA OSIV 등)도 있지만 그 경우 lock 획득 직후 해제되어 다음 update에서는 lock 없는 상태로 동작 → race 보호 실패. **별도 빈 분리(섹션 1)가 이 전제 조건을 보장**.
- **`pg_try_advisory_xact_lock`은 non-blocking** — 즉시 true/false 반환. 다른 replica가 lock 보유 시 대기 없이 skip해 부팅 시간 보장.
- **트랜잭션 단위 자동 해제** — `_xact_` 접미사가 transaction-scoped. commit/rollback 시 자동 해제. session-scoped(`pg_try_advisory_lock`)는 명시적 unlock 필요해 함정.
- **상수 키 충돌 방지** — 본 backfill 전용 키를 정해 다른 advisory lock(다른 모듈, 다른 backfill)과 충돌 회피. 키 영역 분리 정책을 코멘트로 명시.

### 3. try/catch로 부팅 차단 회피 + 다음 부팅 재시도

```java
@Override
public void run(ApplicationArguments args) {
    try {
        executor.run();
    } catch (Exception e) {
        log.error("FAVORITE_PRIORITY_BACKFILL_FAILED next-boot-will-retry", e);
    }
}
```

- **`run()`에 try/catch** — 어떤 예외가 raise되어도 ERROR 로그만 남기고 부팅 자체는 진행. 실패한 replica가 unhealthy로 전체 배포가 멈추는 사고 회피.
- **try/catch 위치는 외부 빈 호출 *바깥*** — Spring AOP 프록시가 commit 단계에서 raise하는 예외도 outer try/catch가 catch한다. inner method `@Transactional`을 같은 클래스 protected에 두는 변형은 AOP 우회 함정에 빠지므로 사용 금지.
- **idempotent 보장이 전제** — 다음 부팅 재시도가 안전하려면 backfill 자체가 idempotent해야 한다. `WHERE priority IS NULL`로 입력을 한정하고 모두 채워지면 즉시 종료하는 패턴이 표준.

### 4. 검증 — DB 명령만으로 backfill 동작 확인

운영 로그를 못 보는 상황에서도 다음 SQL로 backfill 정상 적용 여부를 검증할 수 있다:

```sql
-- A. NULL 행이 줄어드는지 (정상 작동 시 0)
SELECT COUNT(*) FROM user_favorite_indicator WHERE priority IS NULL;

-- B. dense 0..N-1 invariant
WITH grp AS (
  SELECT user_id, source_type,
         COUNT(*) AS cnt, MIN(priority) AS min_p, MAX(priority) AS max_p,
         COUNT(DISTINCT priority) AS distinct_p
  FROM user_favorite_indicator GROUP BY user_id, source_type
)
SELECT * FROM grp
WHERE NOT (min_p = 0 AND max_p = cnt - 1 AND distinct_p = cnt);
-- expect: 0행

-- C. advisory lock 보유 세션 (현재 backfill 진행 중인 인스턴스가 있는지)
SELECT pid, granted, query
FROM pg_locks l LEFT JOIN pg_stat_activity a USING (pid)
WHERE locktype = 'advisory' AND classid = 0 AND objid = 4242042001;
-- 0행: 현재 backfill 진행 중인 인스턴스 없음 (이미 끝났거나 아직 시작 안 함)
```

A에서 NULL이 그대로면 backfill이 작동하지 않은 것 — Spring AOP 우회 함정에 빠졌을 가능성 1순위. 섹션 1의 별도 빈 분리 패턴 적용 여부를 코드에서 즉시 점검.

### 5. 운영 가시성 — 명확한 로그 키워드

```text
FAVORITE_PRIORITY_BACKFILL_APPLIED rows=N         # 정상 적용
FAVORITE_PRIORITY_BACKFILL_SKIPPED reason=...     # 다른 replica가 처리 중 (정상)
FAVORITE_PRIORITY_BACKFILL_FAILED ...             # 실패 — 다음 부팅 재시도
```

각 키워드는 운영 모니터링(ELK/Loki)의 별도 alert 룰로 등록한다. APPLIED는 INFO trend 모니터, SKIPPED는 INFO 정상 신호, FAILED는 ERROR alert로 즉시 escalation.

**중요**: APPLIED 로그가 한 번도 찍히지 않으면 backfill 자체가 실행 안 됐을 가능성 — Spring AOP 우회 함정 의심. 섹션 4의 SQL로 즉시 검증.

## Why This Matters

- **Rolling 배포 안전성**: blue/green 또는 multi-replica 환경에서 backfill race로 데이터 무결성이 깨지면 phase 3 SQL이 실패하고 운영자가 수동 정리해야 한다. 본 패턴으로 race 자체를 차단.
- **부팅 가용성 보존**: 한 replica의 일시적 backfill 실패가 LB의 절반을 잘라먹지 않도록 보장. ApplicationRunner 예외가 부팅을 abort한다는 점을 모르고 try/catch를 빠뜨리면 이 trap이 발생.
- **Spring AOP 함정 회피**: `protected` + 같은 클래스 self-invocation 패턴은 Spring 통념과 달리 `@Transactional`이 적용되지 않는다. 이를 모르면 advisory lock + bulk update가 트랜잭션 외부에서 흩어져 race 보호와 atomicity 모두 실패. 본 PR이 이 함정을 자체 incident로 학습 — *별도 빈 분리는 보일러플레이트가 아니라 정확성의 전제*.
- **3-phase 마이그레이션의 첫 phase 안전성**: 본 repo의 `jpa-version-passthrough-...` 패턴이 phase 1 entity-side 함정을 다룬다면, 본 문서는 phase 1 runtime-side(부팅 시 backfill 동작 안전성)를 다룬다 — 함께 적용해야 phase 1이 완성.

## When to Apply

- ApplicationRunner로 부팅 시 1회 backfill을 실행해야 할 때
- rolling/blue-green 배포로 같은 시점에 여러 replica가 동시에 부팅할 가능성이 있을 때
- backfill 도중 일시적 장애(lock timeout, 네트워크 일시 단절)에 대해 부팅 차단보다 next-boot 재시도가 합리적일 때
- ddl-auto + 수동 SQL 하이브리드 마이그레이션의 phase 1 ~ phase 3 윈도우 동안 UNIQUE 제약 부재 상태로 backfill해야 할 때
- backfill 안에서 advisory lock + bulk update를 단일 트랜잭션 경계 안에 묶어야 할 때 (즉, `@Transactional`이 정확히 적용되어야 할 때)

## Examples

### 함정 시나리오 1 — `protected` inner method + self-invocation (본 PR이 경험한 incident)

```java
// 잘못된 패턴 — 본 PR 초기 작성본
@Component
public class FavoritePriorityBackfillRunner implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) {
        try {
            backfillWithLock();   // ← self-invocation, AOP 우회, @Transactional 미적용
        } catch (Exception e) {
            log.error("...", e);
        }
    }

    @Transactional               // ← 같은 클래스 호출이라 사실상 무시됨
    protected void backfillWithLock() {
        if (!tryAcquireAdvisoryLock()) return;
        // ... bulk update ...
    }
}
```

**실제 결과 (2026-05-10 운영 incident)**:
- `tryAcquireAdvisoryLock`이 lock을 짧은 auto-tx로 획득 후 즉시 해제 → 이후 update에서는 lock 없는 상태
- `entityManager` operation이 트랜잭션 외부에서 흩어짐 → backfill 일부 실패 또는 silent swallow
- 운영자가 NULL priority 행이 그대로 잡혀있음을 발견 → SQL로 수동 backfill 처리
- 다음 부팅 재시도도 같은 함정에 빠지므로 코드 fix 없이는 영구 broken

### 함정 시나리오 2 — try/catch + advisory lock 누락

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

### 정답 패턴 — 별도 빈 분리

위 Guidance 섹션 1의 코드가 정답. 다음 부팅에서 NULL 행이 발견되면 `executor.run()`이 정확히 advise된 트랜잭션 안에서 advisory lock + bulk update를 atomic하게 처리.

### 본 작업 적용 사례 (이력)

본 PR(#42)에서 backfill runner를 도입했으나, **1회성 마이그레이션 작업의 본질** + **운영자가 SQL로 NULL 행을 수동 처리** + **Phase 3 SQL 적용 후 NOT NULL 강제로 미래 NULL 발생 불가**라는 세 조건이 함께 충족되어 결국 backfill runner 자체를 제거하기로 결정함. 본 favorite 모듈에는 현재 적용 코드가 없다 — 본 학습 문서는 *다른 모듈에서 nullable 컬럼 + bootstrap backfill 패턴을 도입할 때* 참고하기 위한 가이드로 유지된다.

- 도입 시점 사용 코드:
  - `src/main/java/com/thlee/stock/market/stockmarket/favorite/infrastructure/config/FavoritePriorityBackfillExecutor.java`(별도 빈)
  - `FavoritePriorityBackfillRunner.java`(thin wrapper, executor 위임)
- advisory lock 키: `4242042001L` (favorite priority backfill 전용 상수)
- 제거 시점: 1회 마이그레이션 완료 + Phase 3 SQL(NOT NULL + UNIQUE DEFERRABLE) 적용 직전. 미래에 또 다른 nullable 컬럼이 도입되면 같은 패턴을 재도입 가능.

**언제 backfill runner를 *제거*해야 하는가** (본 PR이 학습한 운영 원칙):
- backfill 대상 컬럼이 NOT NULL로 격상되어 미래에 NULL이 발생할 수 없을 때
- 신규 행에 priority를 부여하는 별도 경로(예: `INSERT ... SELECT MAX+1`)가 보장되어 있을 때
- 운영 환경에서 NULL 행이 모두 제거되었음이 SQL로 검증되었을 때

세 조건이 모두 충족되면 runner는 영구 no-op이며 dead code다 — 즉시 제거가 코드 위생에 더 좋다. backfill 패턴은 *일회성*이며 dead code로 남겨두는 게 미래 학습자에게 의도 추적 비용을 부과한다.

## Related

- `docs/solutions/architecture-patterns/jpa-version-passthrough-ddl-auto-not-null-backfill-2026-04-28.md` — 같은 3-phase 마이그레이션의 entity-side 함정 (NOT NULL silent skip)
- `docs/solutions/architecture-patterns/deferred-unique-constraint-retry-requires-new-2026-05-10.md` — 본 PR의 retry semantics (REQUIRES_NEW + 별도 빈 분리 패턴 — 본 문서와 같은 정답 패턴 family)
- `docs/solutions/architecture-patterns/external-http-per-item-transaction-isolation-2026-04-26.md` — 같은 self-invocation 함정의 다른 trigger (per-item HTTP 호출)
- `docs/plans/2026-05-07-001-feat-watchlist-priority-and-graph-layout-plan.md` — Issue #42 plan, Unit 1 (backfill runner) + Documentation/Operational Notes (롤백 시나리오)
- 외부 참고:
  - PostgreSQL `pg_try_advisory_xact_lock` 문서
  - Spring 공식 문서: `@Transactional` self-invocation 제약 ("Method visibility and `@Transactional` in proxy mode")
  - Spring `Propagation.REQUIRES_NEW` + AOP self-invocation 회피 패턴
