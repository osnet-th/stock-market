---
title: 외부 스크래핑 병렬 호출의 DoS/장애 격리 패턴 — bounded executor + wall-clock timeout + Caffeine negative cache + graceful shutdown
category: performance-issues
date: 2026-04-23
tags: [concurrency, caffeine, loadingcache, single-flight, completablefuture, executorservice, timeout, graceful-shutdown, negative-cache, trading-economics, dos-prevention]
module: favorite, economics
problem_type: performance
severity: HIGH
symptom: 사용자의 관심 지표를 카테고리 단위로 병렬 스크래핑할 때 (1) 빈 결과가 캐시되지 않아 매 요청마다 외부 HTTP 재시도, (2) `.join()` 이 벽시계 timeout 없이 무한 대기 가능, (3) daemon 스레드라 JVM 종료 시 in-flight 스크래핑이 즉시 잘려 500/connection-reset 유발
root_cause: (1) Caffeine loader 가 null 반환 시 entry 미생성 → 지속 empty 인 지표에서 cold-cache 브루트포스, (2) `CompletableFuture.allOf(...).join()` 은 Tomcat 워커를 영구 점유 가능 → 풀 고갈 DoS, (3) `setDaemon(true)` + `destroyMethod="shutdown"` 조합은 JVM exit 시 진행 중 task 를 즉시 절단
affected_versions: Spring Boot 4 + Caffeine 3.x + Java 21
---

## Symptom

글로벌 경제지표 대시보드에서 관심 지표를 실시간 스크래핑 경유로 전환한 뒤, 다음 3가지가 후행 리뷰에서 드러났습니다.

1. **빈 결과 브루트포스**: 지속적으로 빈 응답을 내는 지표(신규 enum, HTML 구조 변경 등) 하나만 있어도 매 대시보드 요청마다 Trading Economics 재스크래핑 발생. 캐시가 있는데도 효과 없음.
2. **풀 포화 DoS**: fixed pool 4 + `LinkedBlockingQueue(unbounded)` + `.join()` 조합. 공격자가 모든 카테고리를 커버하는 관심 지표를 등록한 뒤 여러 세션에서 `/favorites/enriched` 를 동시 호출하면 4 슬롯 즉시 포화 → 큐 무한 적재 → Tomcat 워커 전면 블로킹.
3. **롤링 배포 시 요청 단절**: SIGTERM 후 Spring context close → executor.shutdown() → daemon=true 스레드가 JVM exit 시 즉시 종료되며 `.join()` 대기 중인 Tomcat 워커가 응답 쓰기에 실패. 정상 응답이 500/connection-reset 으로 관측.

## Root Cause

### 1. Caffeine `LoadingCache` 의 null 반환 계약
Caffeine `CacheLoader` 가 `null` 을 반환하면 "computation is discarded and an entry is not created" — 공식 문서에 명시된 계약. 우리는 초기 구현에서 "빈 결과 미캐시" 요구를 만족시키기 위해 loader 에서 `null` 을 반환했는데, 그 결과 **매 `get(key)` 호출이 loader 를 재호출** 하게 됨.

### 2. `CompletableFuture.allOf(...).join()` 의 벽시계 미설정
`.join()` 은 인터럽트 없이 영구 대기. 외부 HTTP 가 지연되면 Tomcat 워커도 함께 묶임. 풀이 4 고정이고 큐가 unbounded 이면 들어오는 모든 요청이 적재되며 각자 `.join()` 을 대기 → 전체 favorite 기능 마비.

### 3. Daemon 스레드의 JVM exit 동작
`Thread.setDaemon(true)` 로 생성된 스레드는 JVM 이 non-daemon 스레드 종료를 기다리며 exit 할 때 **즉시 종료** 됨. `destroyMethod="shutdown"` 은 신규 task 만 거부할 뿐 진행 중 task 완료를 보장하지 않음.

## Solution

### 1. Caffeine `Expiry` 로 차등 TTL — 빈 결과만 짧게 (Negative Cache)

loader 는 `List.of()` 를 반환하고 **캐시에 저장**. `Expiry` 구현체가 value 크기를 보고 TTL 을 결정.

```java
// economics/infrastructure/global/tradingeconomics/config/GlobalIndicatorCacheConfig.java
public static final Duration TTL = Duration.ofHours(12);
public static final Duration EMPTY_TTL = Duration.ofSeconds(60);  // 빈 결과는 60s 후 자연 만료 → 재시도

// economics/application/GlobalIndicatorCacheService.java
public GlobalIndicatorCacheService(GlobalIndicatorPort port) {
    this.cache = Caffeine.newBuilder()
        .expireAfter(new EmptyAwareExpiry())   // ← 고정 TTL 대신 value 기반 동적 TTL
        .maximumSize(GlobalIndicatorCacheConfig.MAX_SIZE)
        .build(this::loadFromPort);
}

private List<CountryIndicatorSnapshot> loadFromPort(GlobalEconomicIndicatorType type) {
    List<CountryIndicatorSnapshot> loaded = port.fetchByIndicator(type);
    return loaded == null ? List.of() : loaded;   // ← null 금지. 빈 리스트 캐싱
}

private static final class EmptyAwareExpiry
        implements Expiry<GlobalEconomicIndicatorType, List<CountryIndicatorSnapshot>> {
    @Override
    public long expireAfterCreate(K key, List<CountryIndicatorSnapshot> value, long t) {
        return ttlFor(value).toNanos();
    }
    @Override
    public long expireAfterUpdate(K key, List<CountryIndicatorSnapshot> value, long t, long d) {
        return ttlFor(value).toNanos();
    }
    @Override
    public long expireAfterRead(K key, List<CountryIndicatorSnapshot> value, long t, long d) {
        return d;   // read 로 TTL 연장 방지
    }
    private static Duration ttlFor(List<CountryIndicatorSnapshot> v) {
        return (v == null || v.isEmpty())
            ? GlobalIndicatorCacheConfig.EMPTY_TTL
            : GlobalIndicatorCacheConfig.TTL;
    }
}
```

**핵심**: "빈 결과 미캐시" 를 "빈 결과도 캐시하되 짧게" 로 재해석 → loader 재호출 폭주를 막으면서 60s 뒤 자연 만료로 복구 기회 유지.

### 2. 벽시계 timeout + 카테고리 강등

`allOf(...).join()` 대신 `allOf(...).get(timeout)` 로 상한 설정. 미완료 카테고리는 `FailureReason.FETCH` 로 강등해 **partial 응답** 유지.

```java
// favorite/infrastructure/config/GlobalFavoriteExecutorConfig.java
public static final long WALL_CLOCK_TIMEOUT_SECONDS = 8L;

// favorite/application/FavoriteIndicatorService.java#enrichGlobalFavorites
Map<IndicatorCategory, CompletableFuture<Void>> categoryFutures = new EnumMap<>(IndicatorCategory.class);
for (IndicatorCategory category : categories) {
    categoryFutures.put(category, CompletableFuture.runAsync(
        () -> fetchCategoryInto(category, snapshotMap, categoryFailure),
        globalFavoriteFetchExecutor));
}
try {
    CompletableFuture.allOf(categoryFutures.values().toArray(CompletableFuture[]::new))
        .get(WALL_CLOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
} catch (TimeoutException e) {
    categoryFutures.forEach((cat, f) -> {
        if (!f.isDone()) {
            f.cancel(true);
            categoryFailure.putIfAbsent(cat, FailureReason.FETCH);
            log.warn("글로벌 카테고리 조회 벽시계 타임아웃: category={}", cat);
        }
    });
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    cancelPending(categoryFutures, categoryFailure);
} catch (ExecutionException e) {
    log.error("글로벌 카테고리 병렬 조회 예외", e);
    cancelPending(categoryFutures, categoryFailure);
}
```

**핵심**:
- `Map<IndicatorCategory, CompletableFuture>` 구조로 **어느 카테고리가 미완료** 인지 식별.
- 개별 future 내부(`fetchCategoryInto`)에서 `TradingEconomicsFetch/Parse` 는 이미 swallow. 상위 catch 는 **전체 timeout/REE/interrupt** 만 처리.
- 타임아웃된 카테고리의 카드는 프론트에서 `failed + refreshable=true` 재조회 버튼 노출.

### 3. Graceful shutdown — daemon=false + `@PreDestroy`

Spring 의 `destroyMethod="shutdown"` 은 부족. `@PreDestroy` 에서 직접 단계별 종료.

```java
// favorite/infrastructure/config/GlobalFavoriteExecutorConfig.java
@Slf4j
@Configuration
public class GlobalFavoriteExecutorConfig {
    public static final String BEAN_NAME = "globalFavoriteFetchExecutor";
    private static final int POOL_SIZE = 4;
    private static final long SHUTDOWN_GRACE_SECONDS = 5L;
    private ExecutorService executor;

    @Bean(name = BEAN_NAME)   // ← destroyMethod 지정 안 함. @PreDestroy 가 담당.
    public ExecutorService globalFavoriteFetchExecutor() {
        this.executor = Executors.newFixedThreadPool(POOL_SIZE, new NamedThreadFactory());
        return this.executor;
    }

    @PreDestroy
    public void shutdownGracefully() {
        if (executor == null) return;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(SHUTDOWN_GRACE_SECONDS, TimeUnit.SECONDS)) {
                log.warn("Executor 가 {}초 내 종료되지 않아 shutdownNow", SHUTDOWN_GRACE_SECONDS);
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }

    private static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger();
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "global-fav-fetch-" + counter.incrementAndGet());
            t.setDaemon(false);   // ← 핵심. Tomcat graceful shutdown 과 맞물림.
            return t;
        }
    }
}
```

**핵심**:
- `daemon=false` 이므로 JVM exit 전 Spring 의 `@PreDestroy` 가 executor 를 정리.
- `awaitTermination(5s)` 가 in-flight HTTP 호출 완료를 기다려 Tomcat 응답 쓰기 성공률을 높임.
- 5s 초과 시 `shutdownNow()` 로 강제 interrupt. Tomcat graceful timeout 보다 짧게 잡아야 컨테이너가 먼저 죽지 않음.

## Prevention

**병렬 외부 API 호출 체크리스트** — 다음 5가지는 병렬 풀을 쓸 때 항상 확인:

1. **벽시계 timeout 필수**: `.join()` 금지, `.get(timeout)` 사용. timeout 초과 시 부분 결과 + 강등 응답을 돌려줄 수 있게 Map 기반 future 추적.
2. **풀 + 큐 경계 명시**: `newFixedThreadPool` 은 unbounded queue. 공격 표면을 좁히려면 `ThreadPoolExecutor` 로 `ArrayBlockingQueue` + `AbortPolicy` 직접 구성.
3. **Daemon=false + `@PreDestroy`**: 외부 I/O task 는 Tomcat graceful shutdown 과 시간을 맞춰 정리.
4. **Caffeine `CacheLoader` 의 null 반환 피하기**: "빈 값 미캐시" 가 필요하면 loader 는 빈 컬렉션을 반환하고 `Expiry` 로 차등 TTL. null 은 single-flight 이득을 무효화.
5. **하위 HTTP 클라이언트 timeout 고정**: `RestClient` / `HttpClient` 의 connect/read timeout 이 명시되지 않으면 풀 슬롯이 영구 점유 가능. 상위 벽시계 timeout 만으로는 부족하고, 하위 소켓도 함께 풀림이 필요.

## Related

- Plan: `docs/plans/2026-04-21-001-fix-global-favorite-realtime-indicator-plan.md` (Phase 5 + Phase 6)
- Review: `docs/analyzes/favorite/2026-04-21-global-favorite-review.md` (Rev.2 재리뷰 섹션)
- Brainstorm: `docs/brainstorms/2026-04-21-global-favorite-realtime-indicator-brainstorm.md`