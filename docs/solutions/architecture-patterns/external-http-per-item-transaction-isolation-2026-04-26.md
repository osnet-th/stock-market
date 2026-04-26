---
title: "외부 HTTP 호출 배치/이벤트 — per-item 트랜잭션 격리 (executor 빈 분리 패턴)"
category: architecture-patterns
date: 2026-04-26
module: stocknote, scheduler, async
problem_type: best_practice
component: service_object
severity: medium
tags:
  - transaction-isolation
  - per-item-transaction
  - external-http
  - spring-aop
  - self-invocation
  - executor-bean
  - scheduler
  - async-event-listener
  - propagation-required
applies_when: "스케줄러/이벤트 리스너에서 N개 아이템에 대해 외부 HTTP 호출 + 영속화를 반복할 때"
---

# 외부 HTTP 호출 배치/이벤트 — per-item 트랜잭션 격리 (executor 빈 분리 패턴)

## Context

스케줄러나 AFTER_COMMIT 이벤트 리스너가 N건의 아이템(노트, 종목 등) 에 대해 외부 HTTP 호출 → 결과 영속화 를 반복하는 패턴은 흔하다. 단일 `@Transactional` 안에서 루프를 돌면 두 가지 문제가 동시에 터진다:

1. **DB 커넥션 점유 시간 = N × 외부 RTT** — 100노트 × 평균 500ms = 50초 동안 HikariCP 커넥션 1개 점거. HikariCP 풀이 빠르게 고갈되며 일반 사용자 요청 처리에 영향.
2. **per-item 실패가 배치 전체 롤백** — try-catch 가 외부 호출 예외만 격리하더라도 catch 안의 `save` 가 던지는 예외(DB 제약 / 락 타임아웃)는 단일 트랜잭션 rollback-only 마킹 → 같은 배치에서 이미 SUCCESS 처리된 아이템까지 모두 롤백.

stocknote 의 `captureForMarket` 가 정확히 이 안티패턴이었다 (ce-review 2026-04-25 P1 #3, reliability + performance 2-way agreement).

## Guidance

**아이템 1건 처리를 별도 빈 (`...CaptureExecutor`) 으로 분리하고, 외부 호출자(스케줄러/리스너) 에서 트랜잭션을 제거한다. executor 메서드의 `@Transactional` propagation 은 default(REQUIRED) 로 두면, 호출자 컨텍스트에 따라 자동으로 새 트랜잭션을 시작하거나 합류한다.**

### 적용 후 트랜잭션 흐름

| 호출자 | 트랜잭션 | 동작 |
|---|---|---|
| 스케줄러 (트랜잭션 없음) | executor 호출 마다 새 트랜잭션 시작 | per-item 격리 ✅ |
| AFTER_COMMIT 비동기 리스너 (트랜잭션 없음) | 동일 | per-item 격리 ✅ |
| 사용자 manualRetry (`@Transactional` outer) | executor 호출 시 outer 합류 | reset save 결과가 캡처에서 보임 (race-safe) ✅ |

### 코드 패턴

```java
// 1. 신규 executor 빈 — 1건 처리 단위 + 자기 트랜잭션
@Slf4j
@Service
@RequiredArgsConstructor
public class StockNoteSnapshotCaptureExecutor {

    private final StockNoteRepository noteRepository;
    private final StockNotePriceSnapshotRepository snapshotRepository;
    private final StockPriceService stockPriceService;

    @Transactional   // default REQUIRED
    public void captureTarget(SnapshotType type, PendingCaptureTarget target) {
        // 외부 HTTP 호출 + 도메인 변경 + save 를 1건만 처리
        BigDecimal price = fetchCurrentPrice(target);
        // markSuccess / markFailed / save ...
    }
}
```

```java
// 2. 호출자 — @Transactional 제거 + 루프에서 executor 호출 + try-catch 격리
@Slf4j
@Service
@RequiredArgsConstructor
public class StockNoteSnapshotService {

    private final StockNoteSnapshotCaptureExecutor captureExecutor;
    // ...

    // 트랜잭션 없음 — executor 호출 마다 새 트랜잭션 시작 (REQUIRED + outer 부재)
    public void captureForMarket(SnapshotType type, MarketType market, LocalDate asOf) {
        List<PendingCaptureTarget> targets = snapshotRepository.findDueForCapture(type, market, asOf);
        for (PendingCaptureTarget target : targets) {
            try {
                captureExecutor.captureTarget(type, target);   // ← 외부 빈 호출 (AOP 프록시 동작)
            } catch (Exception e) {
                log.warn("per-note isolation: noteId={}, error={}", target.noteId(), e.getMessage());
            }
        }
    }
}
```

### 핵심 원리 3가지

1. **Spring AOP 프록시는 외부 빈 호출 시에만 `@Transactional` 적용**. 같은 빈 안의 self-invocation 은 AOP 우회 → `@Transactional` 무시. → executor 를 별도 빈으로 분리해야 함.
2. **propagation=REQUIRED (default)** — 호출자가 트랜잭션 없으면 새로 시작, 있으면 합류. 이 양면성이 본 패턴의 핵심: 스케줄러는 격리를 원하고, 사용자 흐름(reset 후 캡처) 은 합류를 원한다.
3. **REQUIRES_NEW 가 아닌 REQUIRED 선택** — REQUIRES_NEW 면 outer 트랜잭션 안에서 호출 시 별도 트랜잭션 시작 → outer 의 미커밋 변경(reset) 이 보이지 않음 → race 위험. REQUIRED 가 두 흐름 모두 안전.

## Why This Matters

- **DB 커넥션 점유 단축**: N×RTT → 1×RTT. HikariCP 풀 고갈 위험 제거.
- **per-item 격리**: 한 아이템 실패가 다음 아이템에 전파되지 않음. plan acceptance "한 종목 실패가 배치 전체 중단 안 함" 의도 달성.
- **AsyncDispatcher 패턴 일관**: AFTER_COMMIT 이벤트 리스너의 self-invocation 회피 디자인과 동일 패턴 — 별도 빈 + AOP 프록시.
- **manualRetry 의 race-safety 자동 확보**: outer 트랜잭션 + REQUIRED 합류로 reset save 결과가 캡처에서 보임.

## When to Apply

- 스케줄러/이벤트 리스너가 N개 아이템을 루프 처리 + 각 아이템마다 외부 HTTP 호출이 있을 때
- catch 안의 `save` 가 추가 예외를 던질 가능성이 있을 때 (DB 제약, 락 타임아웃 등)
- 사용자 동기 흐름 (manualRetry 등) 에서도 같은 캡처 로직을 재사용해야 할 때
- 외부 HTTP timeout 이 길거나 미설정인 환경 (HikariCP 점유 위험 큼)

**적용하지 않는 경우**:
- 외부 호출 없이 순수 DB 작업만 있는 배치 (단일 트랜잭션이 더 효율적, 배치 INSERT 가능)
- N=1 또는 매우 작은 N + 트랜잭션 단일성이 명시적으로 필요한 경우

## Examples

### Before — 단일 @Transactional + self-invocation

```java
@Service
@RequiredArgsConstructor
public class StockNoteSnapshotService {

    @Transactional   // ← N건 외부 HTTP 동안 DB 커넥션 점유
    public void captureForMarket(SnapshotType type, MarketType market, LocalDate asOf) {
        List<PendingCaptureTarget> targets = snapshotRepository.findDueForCapture(...);
        for (PendingCaptureTarget target : targets) {
            captureTarget(type, target);   // ← self-invocation, captureTarget 의 @Transactional 무시
        }
    }

    @Transactional   // ← 무시됨 (self-invocation)
    private void captureTarget(SnapshotType type, PendingCaptureTarget target) {
        BigDecimal price = fetchCurrentPrice(target);   // 외부 HTTP
        try {
            // markSuccess + save
        } catch (Exception e) {
            // markFailed + save → 이 save 가 예외 던지면 outer 트랜잭션 rollback-only 마킹
            // → 배치 전체 롤백
        }
    }
}
```

### After — executor 빈 분리 + 호출자 트랜잭션 제거

```java
@Service
@RequiredArgsConstructor
public class StockNoteSnapshotCaptureExecutor {   // ← 신규 빈
    @Transactional   // ← AOP 프록시 적용 (외부 빈 호출이라)
    public void captureTarget(SnapshotType type, PendingCaptureTarget target) {
        // 외부 HTTP + save (per-item 트랜잭션)
    }
}

@Service
@RequiredArgsConstructor
public class StockNoteSnapshotService {

    private final StockNoteSnapshotCaptureExecutor captureExecutor;

    // 트랜잭션 없음
    public void captureForMarket(SnapshotType type, MarketType market, LocalDate asOf) {
        List<PendingCaptureTarget> targets = snapshotRepository.findDueForCapture(...);
        for (PendingCaptureTarget target : targets) {
            try {
                captureExecutor.captureTarget(type, target);   // ← AOP 프록시 → 새 트랜잭션 시작
            } catch (Exception e) {
                log.warn("per-note isolation: ...", e.getMessage());
            }
        }
    }

    // outer 트랜잭션 유지 — executor REQUIRED 가 합류 → reset 결과 보임
    @Transactional
    public void manualRetry(Long noteId, SnapshotType type, Long userId) {
        // 권한 검증 + reset save
        captureExecutor.captureTarget(type, target);   // ← outer 합류, race-safe
    }
}
```

### 동일 패턴이 적용되는 다른 위치

| 케이스 | 호출자 → executor |
|---|---|
| stocknote AFTER_COMMIT AT_NOTE 캡처 | `StockNoteCreatedSnapshotListener` → `executor.captureAtNote` |
| stocknote retryPending 10분 배치 | `StockNoteSnapshotService.retryPending` → `executor.captureAtNote` |
| stocknote 사용자 수동 재시도 | `StockNoteSnapshotService.manualRetry` (outer Tx) → `executor.captureTarget` |

세 케이스 모두 같은 executor 를 호출하지만 트랜잭션 경계는 자동으로 결정됨 (REQUIRED 의 양면성).

## References

- 적용 PR: feat/stocknote (Phase 10 P1 #3 — 2026-04-25 ce-review)
- 분석/설계: `.claude/{analyzes,designs}/stocknote/{capture-transaction-isolation,snapshot-capture-executor}/`
- 관련 학습: [`global-indicator-history-mirroring.md`](./global-indicator-history-mirroring.md) — 스케줄러 종목별 try-catch 격리 패턴 (본 패턴의 호출자 측 보강)
- 관련 학습: [`parallel-external-fetch-resilience-2026-04-23.md`](../performance-issues/parallel-external-fetch-resilience-2026-04-23.md) — 외부 호출 timeout/벽시계 가드