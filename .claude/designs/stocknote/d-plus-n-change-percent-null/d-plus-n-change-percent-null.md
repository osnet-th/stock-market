# [stocknote] D+N changePercent null 영구화 방지 (옵션 C: A+B 결합)

> 분석: [d-plus-n-change-percent-null](../../../analyzes/stocknote/d-plus-n-change-percent-null/d-plus-n-change-percent-null.md). plan task: Phase 10 P1 #6.

## 의도

**옵션 A**: D+N 캡처 시점에 AT_NOTE 가 SUCCESS 가 아니면 (`atNoteClose=null`) markSuccess 호출 자체를 회피 → 신규 changePercent=null SUCCESS 행 생성 차단.
**옵션 B**: AT_NOTE 가 SUCCESS 로 전이되는 시점에 동일 noteId 의 D+N SUCCESS+changePercent=null 행을 보강 → 기존/향후 잔존 행 자동 회복.

## 변경 사항

### 1. 도메인 메서드 추가 — `StockNotePriceSnapshot`

```java
/**
 * SUCCESS 상태 + changePercent 가 null 인 경우에 한해 atNotePrice 기준 변화율을 사후 보강.
 * AT_NOTE 가 D+N 보다 늦게 SUCCESS 된 경우 captureAtNote 가 호출.
 *
 * @return 실제로 보강이 일어났으면 true
 */
public boolean backfillChangePercent(BigDecimal atNotePrice) {
    if (this.status != SnapshotStatus.SUCCESS) {
        return false;
    }
    if (this.changePercent != null) {
        return false; // 이미 계산되어 있음
    }
    if (this.closePrice == null || atNotePrice == null) {
        return false;
    }
    this.changePercent = computeChangePercent(atNotePrice, this.closePrice);
    return this.changePercent != null;
}
```

### 2. `StockNoteSnapshotCaptureExecutor.captureTarget` — 옵션 A

```java
@Transactional
public void captureTarget(SnapshotType type,
                          StockNotePriceSnapshotRepository.PendingCaptureTarget target) {
    Optional<StockNotePriceSnapshot> existing = snapshotRepository
            .findByNoteIdAndType(target.noteId(), type);
    if (existing.isEmpty()) { ... return; }
    StockNotePriceSnapshot snapshot = existing.get();
    if (!snapshot.canRetry()) { return; }

    // 옵션 A: AT_NOTE 가 아직 SUCCESS 가 아니면 D+N 캡처를 미루고 PENDING 유지.
    // changePercent=null SUCCESS 가 영구 저장되는 것을 차단. AT_NOTE 가 늦게 SUCCESS 되면
    // 옵션 B 의 backfill 또는 다음 cron 회차의 재시도(Task #24 후속) 가 처리.
    if (target.atNoteClosePrice() == null) {
        log.info("D+N capture deferred (AT_NOTE not yet SUCCESS): noteId={}, type={}",
                target.noteId(), type);
        return;
    }

    StockNote note = noteRepository.findById(target.noteId()).orElse(null);
    if (note == null) { ... return; }
    try {
        BigDecimal price = fetchCurrentPrice(note);
        snapshot.markSuccess(LocalDate.now(), price, target.atNoteClosePrice());
        snapshotRepository.save(snapshot);
    } catch (Exception e) { ... }
}
```

### 3. `StockNoteSnapshotCaptureExecutor.captureAtNote` — 옵션 B

```java
@Transactional
public void captureAtNote(Long noteId) {
    Optional<StockNotePriceSnapshot> snapshotOpt = snapshotRepository.findByNoteIdAndType(noteId, SnapshotType.AT_NOTE);
    if (snapshotOpt.isEmpty()) { ... return; }
    StockNotePriceSnapshot snapshot = snapshotOpt.get();
    if (snapshot.isSuccess() || snapshot.isRetryExhausted()) { return; }
    Optional<StockNote> noteOpt = noteRepository.findById(noteId);
    if (noteOpt.isEmpty()) { ... return; }
    StockNote note = noteOpt.get();
    try {
        BigDecimal price = fetchCurrentPrice(note);
        int updated = snapshotRepository.markSuccessIfPending(snapshot.getId(),
                LocalDate.now(), price, BigDecimal.ZERO);
        if (updated == 0) {
            log.warn("AT_NOTE snapshot conditional update skipped: id={} (status transitioned)",
                    snapshot.getId());
            return;
        }
        // 옵션 B: AT_NOTE 가 SUCCESS 로 전이되었으니 동일 noteId 의 D+N changePercent 보강.
        backfillDPlusNChangePercent(noteId, price);
    } catch (Exception e) {
        snapshot.markFailed("AT_NOTE capture failed: " + e.getClass().getSimpleName());
        snapshotRepository.save(snapshot);
        log.warn("AT_NOTE capture failed: noteId={}, retryCount={}, reason={}",
                noteId, snapshot.getRetryCount(), e.getMessage());
    }
}

/** AT_NOTE 늦은 SUCCESS 시 동일 noteId 의 D+7/D+30 SUCCESS+null 행 changePercent 보강. */
private void backfillDPlusNChangePercent(Long noteId, BigDecimal atNotePrice) {
    for (SnapshotType type : new SnapshotType[]{SnapshotType.D_PLUS_7, SnapshotType.D_PLUS_30}) {
        snapshotRepository.findByNoteIdAndType(noteId, type)
                .filter(s -> s.backfillChangePercent(atNotePrice))
                .ifPresent(s -> {
                    snapshotRepository.save(s);
                    log.info("D+N changePercent backfilled: noteId={}, type={}", noteId, type);
                });
    }
}
```

## 변경 동작

| 시나리오 | 변경 전 | 변경 후 |
|---|---|---|
| AT_NOTE FAILED + D+7 도달 cron | D+7 SUCCESS + changePercent=null (영구) | D+7 PENDING 유지 (옵션 A) |
| AT_NOTE 가 manualRetry 로 늦게 SUCCESS | D+N 잔존 changePercent=null 그대로 | 동일 트랜잭션에서 D+N 보강 (옵션 B) |
| AT_NOTE SUCCESS 후 D+7 정상 도달 | 정상 | 정상 (영향 없음) |
| AT_NOTE SUCCESS, D+7 도 SUCCESS 이미 정상 (changePercent != null) | n/a | backfillChangePercent 가 false 반환 → save 없음 |

## 회귀 위험

| 위험 | 영향 | 완화 |
|---|---|---|
| D+N 가 PENDING 영구 잔존 | AT_NOTE 가 영구 FAILED 면 D+N 도 영구 PENDING | Task #24 의 retryPending D+N 처리 후 재시도 가능. 사용자 manualRetry(D+N) 도 가능 |
| backfill 호출이 captureAtNote 트랜잭션 시간 증가 | 미미 (D+7/D+30 두 행 lookup + 조건부 save) | n/a |
| AT_NOTE 가 동시 SUCCESS 전이 race | markSuccessIfPending 의 row=0 분기에서 backfill 호출 안 함 (return) | OK |

## 작업 리스트

- [ ] `StockNotePriceSnapshot` 에 `backfillChangePercent(BigDecimal)` 메서드 추가
- [ ] `StockNoteSnapshotCaptureExecutor.captureTarget` 에 atNoteClose=null guard 추가 (옵션 A)
- [ ] `StockNoteSnapshotCaptureExecutor.captureAtNote` 에 markSuccessIfPending 성공 후 backfill 호출 (옵션 B)
- [ ] `StockNoteSnapshotCaptureExecutor` 에 private `backfillDPlusNChangePercent(Long, BigDecimal)` 추가
- [ ] 컴파일 확인
- [ ] plan checkbox 갱신 (P1 #6)

## 승인 대기

태형님 승인 후 구현 진행.