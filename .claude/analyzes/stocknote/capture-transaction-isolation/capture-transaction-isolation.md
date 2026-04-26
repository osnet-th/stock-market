# [stocknote] captureForMarket 단일 @Transactional + 외부 HTTP 직렬화 — 격리 부재

> ce-review 2026-04-25 P1 #3 (reliability + performance 합의). plan task: Phase 10 P1 captureForMarket 트랜잭션 분리.

## 현재 상태

### 단일 @Transactional 안에서 KIS HTTP N건 직렬 호출

`StockNoteSnapshotService.java:89-102` `captureForMarket`

```java
@Transactional
public void captureForMarket(SnapshotType type, MarketType marketType, LocalDate asOfDate) {
    int targetOffset = resolveOffset(type);
    List<...PendingCaptureTarget> targets = snapshotRepository.findDueForCapture(type, marketType, asOfDate);
    for (PendingCaptureTarget target : targets) {
        LocalDate due = businessDayCalculator.addBusinessDays(target.noteDate(), targetOffset);
        if (marketType.isDomestic() && !due.isEqual(asOfDate)) continue;
        captureTarget(type, target);   // 내부에서 KIS HTTP 호출
    }
}
```

`captureTarget` 은 `fetchCurrentPrice` (KIS HTTP) → `markSuccess`/`markFailed` → `save` 를 한 트랜잭션 안에서 직렬 처리.

### 문제 두 가지

#### 1. DB 커넥션 점유 = N × KIS RTT

100노트 × 평균 500ms = 50초 동안 HikariCP 커넥션 1개 점거. 다중 시장(국내+해외) × D+7+D+30 = 4개 스케줄러 동시 실행 시 4개 커넥션 1분 동안 묶여 일반 사용자 요청 처리 영향.

#### 2. per-note 실패가 배치 전체 롤백

`captureTarget` 의 try-catch 가 외부 호출 예외만 격리하고 catch 안의 `snapshot.save(...)` 가 예외 던지면(예: DB 제약 위반 / 락 타임아웃) 단일 트랜잭션 rollback-only 마킹 → 같은 배치에서 이미 SUCCESS 처리된 다른 종목까지 모두 롤백. plan acceptance "한 종목 실패가 배치 전체 중단 안 함" 의도와 모순.

## 영향 범위

| 시나리오 | 동작 |
|---|---|
| 정상 100노트 배치 | 50초+ DB 커넥션 점유. 사용자 요청 latency 영향 |
| 한 종목 save 실패 | 99건 SUCCESS 롤백 → KIS 호출 비용 낭비 + 다음 cron 까지 대기 |
| HikariCP 풀 부족 | 다른 도메인 요청도 커넥션 대기 |
| KIS 일시 장애 + 재시도 | 트랜잭션 점유 시간 추가 증가 |

## 해결 옵션

### 옵션 A — captureTarget/captureAtNote 를 별도 빈으로 분리 (권장)

신규 `StockNoteSnapshotCaptureExecutor` 빈에 두 메서드를 옮긴다. 각 메서드는 자기 `@Transactional` (default REQUIRED) 보유. SnapshotService 의 `captureForMarket`/`retryPending` 은 `@Transactional` 제거 + 루프에서 executor 호출 → **트랜잭션 없는 외부 호출자 → executor 호출 마다 새 트랜잭션 시작 → per-note 격리**.

| 장점 | 단점 |
|---|---|
| per-note 트랜잭션 격리 (한 노트 실패가 다른 노트 무영향) | 신규 빈 1개 추가 |
| DB 커넥션 점유 = 1건 KIS RTT (각 노트마다 새 커넥션) | self-invocation 회피로 약간의 코드 이동 |
| AsyncDispatcher 의 self-invocation 제약과 동일 패턴 (검증된 디자인) | |
| Spring AOP 프록시 자연 동작 | |
| manualRetry 는 outer @Transactional 유지 (propagation REQUIRED 합류) — reset save 결과가 캡처에서 보임 | |

### 옵션 B — 외부 호출 결과 메모리 수집 후 일괄 영속화

`captureForMarket` 가 KIS 호출만 먼저 모두 수행 → 결과 List 보관 → 마지막에 짧은 트랜잭션으로 saveAll.

| 장점 | 단점 |
|---|---|
| 외부 호출 시간이 트랜잭션 밖 | 큰 리팩토링 (markSuccess/markFailed 도메인 메서드 분리) |
| | 한 노트 실패 시 처리가 복잡 (개별 markFailed vs 일괄 save) |
| | retryCount 누적 의미 약화 |

### 옵션 C — REQUIRES_NEW + manualRetry 영향

executor 를 REQUIRES_NEW 로 강제하면 manualRetry 의 outer `@Transactional` 안에서 호출 시 별도 트랜잭션 시작 → outer 의 reset save 결과가 새 트랜잭션에서 보이지 않음 (READ_COMMITTED 기본) → `markSuccessIfPending` 의 WHERE status='PENDING' 조건이 안 맞을 위험.

→ **REQUIRES_NEW 채택 안 함**. 옵션 A (REQUIRED) 채택.

## 추천: 옵션 A

근거:
- 변경 범위 적정 (executor 빈 + 메서드 이동)
- per-note 격리 확보
- DB 커넥션 점유 시간 단축
- manualRetry 의 reset → 캡처 흐름이 같은 트랜잭션에서 자연 동작 (Task #21 의 self-invocation 회피와도 정합)
- AsyncDispatcher 패턴(별도 빈 + AOP 프록시) 과 일관

## 코드 위치

| 파일 | 라인 | 변경 |
|---|---|---|
| `StockNoteSnapshotService.java` | 47-83 | `captureAtNote` 제거 (executor 로 이동) |
| 〃 | 89-102 | `captureForMarket` `@Transactional` 제거 + executor 호출 |
| 〃 | 138-147 | `retryPending` `@Transactional` 제거 + executor 호출 |
| 〃 | 111-131 | `manualRetry` outer `@Transactional` 유지 + executor 호출 |
| 〃 | 149-176 | `captureTarget` 제거 (executor 로 이동) |
| `StockNoteSnapshotCaptureExecutor.java` | 신규 | 두 메서드 보유 |
| `StockNoteSnapshotAsyncDispatcher.java` | - | `SnapshotService.captureAtNote` → `executor.captureAtNote` 호출 변경 |

## 후속 task 와의 관계

| Task | 정합 |
|---|---|
| #21 P2 manualRetry self-invocation 트랜잭션 분리 | 본 task 가 captureTarget/captureAtNote 를 executor 로 분리하면 manualRetry 의 self-invocation 자체가 해소됨 → Task #21 의 일부 자동 해결. 잔여 (reset save 트랜잭션 분리) 는 Task #21 에서 처리. |
| #22 P2 AsyncConfig CallerRunsPolicy → AbortPolicy | 본 task 와 독립. |

## 설계 문서

[snapshot-capture-executor](../../../designs/stocknote/snapshot-capture-executor/snapshot-capture-executor.md)