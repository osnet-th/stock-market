---
title: "다중 출처 어댑터 배치의 capacity·throttle·timeout 3중 cascade 차단 패턴"
category: architecture-patterns
date: 2026-05-10
module: realestate, scheduler, async, source-adapter
problem_type: best_practice
component: background_job
severity: high
related_components:
  - scheduler
  - async_executor
  - source_adapter
  - admin_controller
  - rest_client
applies_when:
  - "다중 출처(N≥3) 외부 API를 한 도메인이 통합 어댑터링하며 일배치로 region × adapter × category 매트릭스 호출"
  - "scheduler가 CompletableFuture.supplyAsync로 task fan-out 수행"
  - "어댑터별 적용 region 범위가 다른 경우(지역 특화 공공 API 등)"
  - "운영자 manual batch trigger endpoint가 동일 배치 파이프라인을 재사용"
  - "외부 API의 응답 시간이 가변적 또는 장애 가능성 (= 거의 모든 외부 API)"
symptoms:
  - "RejectedExecutionException으로 batch 전체 abort, success/failure/savedRows 메트릭 미기록"
  - "admin runBatch 엔드포인트가 7분+ Tomcat worker 점유, retry 시 cron까지 cascade abort"
  - "future.cancel(true) 이후에도 좀비 워커가 socket read를 점유해 graceful shutdown 차단"
root_cause: config_error
resolution_type: code_fix
tags:
  - batch-executor-capacity
  - region-prefix-filter
  - apache-httpclient
  - admin-async-trigger
  - in-flight-guard
  - caller-runs-policy
  - rest-client-timeout
  - scheduler-cascade
---

# 다중 출처 어댑터 배치의 capacity·throttle·timeout 3중 cascade 차단 패턴

## Context

단일 외부 출처(예: ECOS) 도메인에서 검증된 표준 패턴(`core 4 / max 8 / queue 200 / AbortPolicy`)을 다중 출처(realestate 8 출처) 도메인에 그대로 적용하면, 표면상 동작하는 듯 보이지만 운영 첫 일배치에 3가지 함정이 cascade로 얽혀 batch가 silently 일부만 처리하고 종료한다.

- region(56) × adapter × supportedCategories(12) = 최대 672 submit
- ECOS 패턴: queue 200 → 다중 출처 산정으로 부족 → 209번째 submit이 RejectedExecutionException
- Admin trigger가 같은 파이프라인을 동기 호출 → cron과 풀 충돌
- 공유 RestClient bean에 timeout 부재 → 좀비 워커 누적

`/ce:review` (issue #41 PR 직전) 시점에 발견. 운영 첫 일배치에 발현 가능. 본 문서는 3가지 함정을 cascade로 묶어 한 번에 차단하는 패턴 묶음.

## Guidance

### 1) Executor 큐 capacity 산정 — 단일 방어선 금지

**문제**: ECOS는 56 region × 1 카테고리 = 56 submit, queue 200으로 충분. 다중 출처는 56 × 12 = 672로 209번째 submit이 `RejectedExecutionException` → AbortPolicy가 항목 단위 거부로 끝나지 않고 batch 통째 abort.

**산정 공식**:
```
queueCapacity ≥ Σ(region × adapter.supportedCategories.size()) × 1.3
```
realestate 산정:
- 총 매트릭스: 56 × 12 = 672
- region prefix 사전 필터로 헛 submit(서울 어댑터 × 비-서울 region 등) 56건 차단 → 실 submit 약 616건
- 안전계수 30% 적용 → queueCapacity 800

**다중 방어선** (단일 정책 의존 금지):
1. `CallerRunsPolicy` — 큐 포화 시 호출자(scheduler) 스레드가 직접 실행 → 자연 throttle
2. submit 단위 try-catch — `RejectedExecutionException` 시 `completedFuture(-1)`로 항목 단위 격리
3. region prefix 사전 필터 (`supportsRegion(region)`) — 헛 submit 자체를 차단

**인터페이스 사전 필터 패턴** — 어댑터 내부 가드를 인터페이스 default로 끌어올려 단일 책임:

```java
// domain/service/RealEstateMarketSourceAdapter.java
public interface RealEstateMarketSourceAdapter {
    RealEstateMarketSource supportedSource();
    Set<RealEstateMarketCategory> supportedCategories();

    /** 해당 출처가 처리할 수 있는 region인지. 기본 true. 지역 특화 출처만 override. */
    default boolean supportsRegion(RegionCode region) { return true; }

    FetchResult fetch(RegionCode region, RealEstateMarketCategory category, FetchWindow window);
}

// SeoulOpenDataAdapter — region prefix 11만 true
@Override public boolean supportsRegion(RegionCode region) { return region.isSeoul(); }

// GgDataDreamAdapter — region prefix 41만 true
@Override public boolean supportsRegion(RegionCode region) { return region.isGyeonggi(); }

// 스케줄러 루프 — 어댑터 fetch 진입 전 차단
for (RealEstateMarketSourceAdapter adapter : adapters) {
    if (!adapter.supportsRegion(regionCode)) continue;
    for (RealEstateMarketCategory category : adapter.supportedCategories()) {
        futures.add(submit(adapter, regionCode, category, window));
    }
}
```

### 2) Admin manual trigger — 즉시 ack + 비동기 dispatch

**문제**: `AdminController`가 `batchScheduler.runDailyBatch()`를 동기 호출하면 javadoc의 "비동기 ack" 약속과 다르게 Tomcat HTTP worker에서 7분+ 동기 실행. retry 시 cron과 같은 fetchExecutor 큐를 공유하므로 cron까지 cascade abort.

**패턴**:
- `application/BatchTriggerService` — presentation → application → infrastructure 의존성 정합. Controller가 infrastructure scheduler를 직접 보지 않게.
- `AtomicBoolean` in-flight guard — 동시 호출 차단(`compareAndSet`), 거부 시 409 Conflict
- 별도 admin executor (single-thread, `queueCapacity 0`, AbortPolicy) — dispatch 전용
- 실제 fetch는 cron과 동일한 `fetchExecutor` 풀 공유 — admin executor는 "스케줄러 실행 자체"만 담당
- 응답은 즉시 202 Accepted + `triggeredAt`

```java
// application/RealEstateBatchTriggerService.java
public TriggerResult trigger() {
    if (!inFlight.compareAndSet(false, true)) {
        return TriggerResult.rejected("another batch in flight");
    }
    adminBatchExecutor.execute(() -> {
        try { batchScheduler.runDailyBatch(); }
        finally { inFlight.set(false); }
    });
    return TriggerResult.triggered();
}

// presentation/RealEstateAdminController.java
@PostMapping("/batch/run")
public ResponseEntity<TriggerResult> runBatch() {
    TriggerResult result = triggerService.trigger();
    if ("rejected".equals(result.status())) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
    }
    return ResponseEntity.accepted().body(result);  // 202
}
```

### 3) 외부 API용 RestClient — 도메인 전용 + Apache HttpClient 5

**문제**: 공유 `RestClient` bean(예: news 도메인용)에는 timeout이 설정되지 않음. 더 본질적으로 JDK `HttpURLConnection`은 socket read 중 thread interrupt를 무시 → `future.cancel(true)`이 좀비 워커를 정리 못 함 → fetchExecutor 풀 점진 고갈 → graceful shutdown 차단 → SIGKILL 강제.

**패턴**:
- 공유 RestClient bean 의존 금지 — timeout 정책은 도메인마다 다르므로 도메인 전용 bean
- `HttpComponentsClientHttpRequestFactory` + Apache HC5 — socket close 보장(interrupt 무시 회피)
- 명시 timeout: connect 5s / response 30s
- 8 어댑터에서 `@Qualifier(RealEstateRestClientConfig.CLIENT_BEAN)` 명시 주입
- `@RequiredArgsConstructor` Lombok과 `@Qualifier` 충돌 회피를 위해 직접 생성자 작성

```java
// infrastructure/config/RealEstateRestClientConfig.java
@Bean(name = CLIENT_BEAN)
public RestClient realEstateRestClient() {
    HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
            .setMaxConnTotal(50).setMaxConnPerRoute(10).build();
    RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(Timeout.ofSeconds(5))
            .setResponseTimeout(Timeout.ofSeconds(30))
            .build();
    CloseableHttpClient httpClient = HttpClients.custom()
            .setConnectionManager(cm)
            .setDefaultRequestConfig(requestConfig)
            .build();
    return RestClient.builder()
            .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
            .build();
}

// 어댑터에서 명시 주입
public SeoulOpenDataAdapter(@Qualifier(RealEstateRestClientConfig.CLIENT_BEAN) RestClient restClient,
                            RealEstateMarketProperties properties) { ... }
```

## Why This Matters

- batch가 silently 일부만 처리하고 종료 → "다음 cron(24h)까지 stale"이 운영 알람도 없이 진행. 일부 region/category만 갱신되므로 UI 표면상 정상 동작처럼 보임.
- admin retry가 cron 큐 포화를 가속화 → cascade의 cascade. 운영자가 "왜 안 되지?" 누르는 버튼이 다음 정기 batch까지 abort.
- 좀비 워커는 graceful shutdown 차단 → SIGKILL 강제 → 재시작 후 같은 시나리오 반복. 좀비 누적이 풀 고갈로 이어짐.

3가지가 따로따로면 운영자가 알아챌 수 있지만, cascade로 얽히면 1차 증상(SIGKILL 후 재시작)만 보고 다른 두 함정을 건드릴 동기가 없어 잠복한다.

## When to Apply

- 다중 출처(N≥3) 외부 API를 한 도메인이 통합 어댑터링
- region × adapter × category 매트릭스 batch 패턴
- 어댑터별 적용 region 범위가 다름 (지역 특화 공공 API)
- 운영자 manual batch trigger endpoint 존재
- 외부 API의 응답 시간이 가변적 또는 장애 가능성

## Examples

### Before (P0 cascade 유발)

| 위치 | 코드 |
|------|------|
| `RealEstateMarketAsyncConfig` | `queueCapacity 200` + `AbortPolicy`. 209번째 submit 거부 → batch abort |
| `RealEstateAdminController` | `batchScheduler.runDailyBatch()` 동기 호출 — Tomcat worker 7분 점유 |
| 어댑터 8종 | `news` 도메인의 공유 `RestClient` bean (timeout 없음) 주입 |
| `RealEstateMarketBatchScheduler.submit` | try-catch 없음 → `RejectedExecutionException`이 batch 중단으로 직결 |
| `SeoulOpenDataAdapter`, `GgDataDreamAdapter` | 어댑터 fetch 내부에서만 region prefix 가드 — 이미 submit 후 거른다 |

### After (Unit 9 fix — commit `f2387d1`)

| 위치 | 코드 |
|------|------|
| `RealEstateMarketAsyncConfig` | `queueCapacity 800` + `CallerRunsPolicy` (fetchExecutor) + 별도 single-thread `adminBatchExecutor` bean |
| `RealEstateAdminController` | `RealEstateBatchTriggerService.trigger()` 호출 → 즉시 202/409 ack |
| `RealEstateBatchTriggerService` | `AtomicBoolean` in-flight guard + adminBatchExecutor dispatch |
| `RealEstateRestClientConfig` (신규) | 도메인 전용 Apache HC5 RestClient (`connect 5s / response 30s`) |
| 8개 어댑터 | `@Qualifier(RealEstateRestClientConfig.CLIENT_BEAN)` 명시 주입 + 직접 생성자 |
| `RealEstateMarketSourceAdapter` 인터페이스 | `default boolean supportsRegion(RegionCode region) { return true; }` 추가 |
| `SeoulOpenDataAdapter`, `GgDataDreamAdapter` | `supportsRegion` override (`isSeoul()` / `isGyeonggi()`) — 어댑터 내부 region 가드 제거 |
| `RealEstateMarketBatchScheduler.submit` | `try { CompletableFuture.supplyAsync(..., fetchExecutor) } catch (RejectedExecutionException) { return CompletableFuture.completedFuture(-1); }` |
| `RealEstateMarketBatchScheduler.runDailyBatch` 루프 | `if (!adapter.supportsRegion(regionCode)) continue;` 사전 필터 |

## Trade-offs

- **queue capacity 단순 확장 vs region prefix 사전 필터**: 후자가 정확한 fix(헛 submit 자체를 차단). 전자는 일시적 미루기. 인터페이스 1줄 default 추가 비용으로 정공법 채택.
- **Apache HC5 의존성 추가**: `build.gradle` 1줄. 재시작 시 새 라이브러리 로드 부담 미미. socket close 보장 + interrupt 작동 이득이 압도적.
- **presentation→infrastructure 직접 의존 제거 → application BatchTriggerService 신설**: 1 클래스 추가 비용으로 의존성 방향 정합 + in-flight guard + admin executor 분리 동시 획득.
- **adminBatchExecutor 별도 single-thread**: cron의 `fetchExecutor`와 admin trigger의 dispatch executor가 같으면 retry 폭주 시 cron 큐를 잡아먹는다. dispatch와 fetch를 풀로 분리하면 admin 폭주가 cron 큐 점유에 영향 없고, 실 fetch는 cron과 같은 fetchExecutor 공유로 어댑터 측 동시성 정책 일관.

## See Also

- [`docs/solutions/performance-issues/parallel-external-fetch-resilience-2026-04-23.md`](../performance-issues/parallel-external-fetch-resilience-2026-04-23.md) — 동일 패턴의 사용자 동기 호출 버전. 본 fix는 batch+admin 면에서 같은 체크리스트(벽시계 timeout, bounded executor, 빈 응답 차등 TTL)를 재적용한 사례연구.
- [`docs/solutions/architecture-patterns/realestate-public-open-api-pitfalls-2026-05-07.md`](./realestate-public-open-api-pitfalls-2026-05-07.md) — 본 fix의 직계 선행 학습. 같은 도메인의 envelope/인증/포맷 함정. **§6 "queue 200 / AbortPolicy" 권고가 본 fix에서 queue 800 / CallerRunsPolicy로 진화 — refresh 권장.**
- [`docs/solutions/architecture-patterns/external-http-per-item-transaction-isolation-2026-04-26.md`](./external-http-per-item-transaction-isolation-2026-04-26.md) — captureExecutor per-item Tx 격리 (전제). 본 fix는 그 위에 큐/timeout/admin ack 레이어를 얹음.

## Sources

- Worktree: `feat/issue-41-real-estate-market-data` (commit `f2387d1` — Unit 9 P0 fix bundle)
- Plan: `docs/plans/2026-05-06-001-feat-real-estate-market-data-plan.md` Unit 9
- Review: `/ce:review` 결과 P0 #1, #2, #3 (3-way agreement: correctness + adversarial + learnings)
