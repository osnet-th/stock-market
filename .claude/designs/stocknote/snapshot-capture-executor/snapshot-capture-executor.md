# [stocknote] StockNoteSnapshotCaptureExecutor 분리 (옵션 A)

> 분석: [capture-transaction-isolation](../../../analyzes/stocknote/capture-transaction-isolation/capture-transaction-isolation.md). plan task: Phase 10 P1 captureForMarket 트랜잭션 분리.

## 의도

`captureAtNote` 와 `captureTarget` 을 신규 `StockNoteSnapshotCaptureExecutor` 빈으로 옮겨 **외부 빈 경유 호출 → AOP 프록시 동작 → per-note 트랜잭션 격리** 를 확보. SnapshotService 의 `captureForMarket`/`retryPending` 은 `@Transactional` 을 제거해 외부 호출자가 됨 → executor 호출마다 새 트랜잭션 시작.

## 변경 사항

### 1. 신규 빈 `StockNoteSnapshotCaptureExecutor`

위치: `stocknote/application/StockNoteSnapshotCaptureExecutor.java`

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class StockNoteSnapshotCaptureExecutor {

    private final StockNoteRepository noteRepository;
    private final StockNotePriceSnapshotRepository snapshotRepository;
    private final StockPriceService stockPriceService;

    /**
     * AT_NOTE PENDING 스냅샷 1건 캡처 (이벤트/재시도/수동재시도 공통 진입점).
     * 자기 트랜잭션을 가져 호출자 컨텍스트에 따라 새 트랜잭션 시작 또는 합류.
     */
    @Transactional
    public void captureAtNote(Long noteId) {
        // 기존 SnapshotService.captureAtNote 본문 그대로 이전
    }

    /**
     * D+7 / D+30 PENDING 스냅샷 1건 캡처.
     */
    @Transactional
    public void captureTarget(SnapshotType type,
                              StockNotePriceSnapshotRepository.PendingCaptureTarget target) {
        // 기존 SnapshotService.captureTarget 본문 그대로 이전
    }

    private BigDecimal fetchCurrentPrice(StockNote note) {
        // 기존 SnapshotService.fetchCurrentPrice 본문 그대로 이전
    }
}
```

`@Transactional` propagation 은 default(REQUIRED). 외부 호출자(captureForMarket / retryPending) 가 트랜잭션 없이 호출하면 새 트랜잭션 시작. manualRetry 처럼 outer 트랜잭션이 있으면 합류 — reset save 결과가 캡처 단계에서 보임.

### 2. `StockNoteSnapshotService` 수정

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class StockNoteSnapshotService {

    private final StockNotePriceSnapshotRepository snapshotRepository;
    private final StockNoteRepository noteRepository;
    private final BusinessDayCalculator businessDayCalculator;
    private final StockNoteSnapshotCaptureExecutor captureExecutor;  // 신규 의존

    // captureAtNote / captureTarget / fetchCurrentPrice 제거 (executor 로 이전)

    /** captureForMarket — @Transactional 제거. 루프에서 executor 호출 (새 트랜잭션). */
    public void captureForMarket(SnapshotType type, MarketType marketType, LocalDate asOfDate) {
        int targetOffset = resolveOffset(type);
        List<...PendingCaptureTarget> targets = snapshotRepository.findDueForCapture(type, marketType, asOfDate);
        for (PendingCaptureTarget target : targets) {
            LocalDate due = businessDayCalculator.addBusinessDays(target.noteDate(), targetOffset);
            if (marketType.isDomestic() && !due.isEqual(asOfDate)) continue;
            try {
                captureExecutor.captureTarget(type, target);
            } catch (Exception e) {
                // executor 트랜잭션이 롤백되더라도 다음 노트로 격리.
                log.warn("captureForMarket per-note isolation: noteId={}, error={}",
                        target.noteId(), e.getMessage());
            }
        }
    }

    /** retryPending — @Transactional 제거. 루프에서 executor 호출. */
    public void retryPending() {
        List<StockNotePriceSnapshot> retryable = snapshotRepository.findRetryable(
                SnapshotStatus.PENDING, StockNotePriceSnapshot.MAX_RETRY, RETRY_BATCH_LIMIT);
        for (StockNotePriceSnapshot snapshot : retryable) {
            if (snapshot.getSnapshotType() == SnapshotType.AT_NOTE) {
                try {
                    captureExecutor.captureAtNote(snapshot.getNoteId());
                } catch (Exception e) {
                    log.warn("retryPending per-note isolation: noteId={}, error={}",
                            snapshot.getNoteId(), e.getMessage());
                }
            }
        }
    }

    /** manualRetry — outer @Transactional 유지. executor 호출 시 propagation REQUIRED 로 합류. */
    @Transactional
    public void manualRetry(Long noteId, SnapshotType type, Long userId) {
        StockNote note = noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new StockNoteNotFoundException(noteId));
        StockNotePriceSnapshot snapshot = snapshotRepository.findByNoteIdAndType(noteId, type)
                .orElseThrow(() -> new IllegalArgumentException("snapshot not found: " + type));
        snapshot.resetForManualRetry();
        snapshotRepository.save(snapshot);

        if (type == SnapshotType.AT_NOTE) {
            captureExecutor.captureAtNote(noteId);
            return;
        }
        BigDecimal atNoteClose = snapshotRepository.findByNoteIdAndType(noteId, SnapshotType.AT_NOTE)
                .filter(StockNotePriceSnapshot::isSuccess)
                .map(StockNotePriceSnapshot::getClosePrice)
                .orElse(null);
        captureExecutor.captureTarget(type, new ...PendingCaptureTarget(
                note.getId(), note.getStockCode(), note.getMarketType(), note.getNoteDate(), atNoteClose));
    }

    private static int resolveOffset(SnapshotType type) { ... }
}
```

### 3. `StockNoteSnapshotAsyncDispatcher` 수정

기존: `snapshotService.captureAtNote(event.noteId())`
변경: `captureExecutor.captureAtNote(event.noteId())`

executor 의 `@Transactional` 이 적용되어 AFTER_COMMIT 비동기 컨텍스트에서 새 트랜잭션 시작.

## 격리 검증 (변경 후 동작)

| 흐름 | 트랜잭션 |
|---|---|
| AsyncDispatcher (POST 직후 비동기) | executor 호출 마다 새 트랜잭션 (outer 없음) |
| captureForMarket (스케줄러) | 루프에서 each call 마다 새 트랜잭션 |
| retryPending (스케줄러) | 동일 |
| manualRetry (사용자) | outer 트랜잭션. executor 호출 시 합류 → reset 결과 보임 |

## 부작용 / 회귀 위험

| 위험 | 영향 | 완화 |
|---|---|---|
| captureForMarket 의 @Transactional 제거 후 findDueForCapture(JPQL native) 호출이 트랜잭션 없이 실행 | Spring Data JPA 의 native 쿼리는 트랜잭션 없어도 동작 (Hibernate Session 자동 생성/종결) | 운영 환경에서 동작 검증 |
| executor 호출 안의 `noteRepository.findById` (captureTarget L161) | 같은 트랜잭션 안이라 read 가능. 변경 없음 | n/a |
| manualRetry outer 트랜잭션 안에서 executor 두 번 호출 (캡처+AT_NOTE 종가 조회+captureTarget) | 모두 같은 트랜잭션 합류 | 정상 |
| 한 노트 트랜잭션 실패 시 retryCount 증가가 반영 안 될 수 있음 | catch 안의 markFailed save 도 같은 트랜잭션 → 롤백되면 retryCount 증가 사라짐 | 다음 cron 회차에 재시도 — retry 한도 의미 약화 가능. Task #24 retryPending D+N 처리 와 함께 별도 검토 |
| 스케줄러 catch 블록 위치 | executor 호출이 던지는 예외는 모두 try-catch 로 격리 | 한 노트 실패가 다음 노트로 전파 안 됨 ✅ |

## 작업 리스트

- [ ] `stocknote/application/StockNoteSnapshotCaptureExecutor.java` 신설 (captureAtNote / captureTarget / fetchCurrentPrice 이동)
- [ ] `StockNoteSnapshotService` 의 captureAtNote / captureTarget / fetchCurrentPrice 제거
- [ ] `StockNoteSnapshotService` 의 captureForMarket / retryPending @Transactional 제거 + executor 호출 + try-catch 추가
- [ ] `StockNoteSnapshotService` 의 manualRetry 내부 self-call 을 executor 호출로 변경
- [ ] `StockNoteSnapshotAsyncDispatcher` 의 SnapshotService 의존 → executor 의존 변경 (또는 SnapshotService 가 executor 위임)
- [ ] 컴파일 확인 (`./gradlew compileJava`)
- [ ] plan checkbox 갱신 (P1 #3)

## 승인 대기

태형님 승인 후 구현 진행.