# [stocknote] D+7/D+30 PENDING 자동 생성 (옵션 A)

> 분석: [d-plus-n-pending-missing](../../../analyzes/stocknote/d-plus-n-pending-missing/d-plus-n-pending-missing.md). plan task: Phase 10 P0 #1.

## 의도

`StockNoteWriteService.create` 가 노트 생성 트랜잭션 안에서 **AT_NOTE + D+7 + D+30 = 3행 PENDING** 을 동시 insert. 스케줄러/manualRetry 의 기존 UPDATE 흐름이 자연스럽게 동작.

## 변경 사항

### 1. `StockNoteWriteService.create` (단일 변경 지점)

**변경 전** (`StockNoteWriteService.java:69`):
```java
snapshotRepository.save(StockNotePriceSnapshot.createPending(noteId, SnapshotType.AT_NOTE));
```

**변경 후**:
```java
snapshotRepository.saveAll(List.of(
        StockNotePriceSnapshot.createPending(noteId, SnapshotType.AT_NOTE),
        StockNotePriceSnapshot.createPending(noteId, SnapshotType.D_PLUS_7),
        StockNotePriceSnapshot.createPending(noteId, SnapshotType.D_PLUS_30)
));
```

기존 `snapshotRepository.saveAll(List<StockNotePriceSnapshot>)` 포트는 이미 정의됨 (`StockNotePriceSnapshotRepository.java:25`). 추가 인터페이스 신설 불필요.

### 2. 검증 흐름 (변경 없음, 회귀 확인 항목)

- `findDueForCapture` (JpaRepository:56-72): D_PLUS_7 / D_PLUS_30 PENDING 행이 이제 존재 → 영업일 도달 시 정상 매칭. 쿼리 변경 없음.
- `captureForMarket.captureTarget` (Service:149-176): markSuccess / markFailed 동작 그대로.
- `manualRetry` (Service:111-131): findByNoteIdAndType(D_PLUS_7) 이 행을 찾음 → resetForManualRetry → captureTarget 정상.
- `markSuccessIfPending` (JpaRepository:39-50): WHERE status='PENDING' 조건 충족.

### 3. unique 제약 회귀 확인

`StockNotePriceSnapshotEntity` 의 `uk_stock_note_snapshot_note_type` (note_id, snapshot_type) 가 동일 type 중복을 막음. saveAll 3건은 모두 다른 SnapshotType 이라 충돌 없음.

### 4. AT_NOTE 비동기 캡처 이벤트 흐름 (변경 없음)

`eventPublisher.publishEvent(new StockNoteCreatedEvent(noteId))` → `StockNoteSnapshotAsyncDispatcher.onStockNoteCreated` → `StockNoteSnapshotService.captureAtNote`. AT_NOTE PENDING 행만 처리하므로 D+7/D+30 PENDING 의 동시 존재가 부작용 없음.

## 작업 리스트

- [ ] `StockNoteWriteService.create` 의 snapshot save 라인을 saveAll 3건으로 변경
- [ ] 컴파일 확인 (`./gradlew compileJava`)
- [ ] plan checkbox 갱신 (P0 #1)

## 영향 받는 후속 task (정합성 확인)

| Task | 정합 여부 |
|---|---|
| #6 P1 AT_NOTE FAILED 시 D+N changePercent null | 본 fix 후 D+N 행 존재로 가시화 → 별도 task 에서 처리 |
| #24 P2 retryPending 가 D+7/D+30 도 처리 | 본 fix 후 D+N PENDING 행 존재 → retryPending 분기 추가 의미 발생 |
| #5 P1 BusinessDayCalculator 음력 공휴일 | 휴장일 KIS 호출 → markFailed 로 retryCount 누적 — 별개 이슈 |

## 회귀 위험

| 위험 | 영향 | 완화 |
|---|---|---|
| D+30 까지 ~30 영업일 PENDING 잔존 → 인덱스 idx_stock_note_snapshot_status_type 에 PENDING 행 누적 | 사용자당 월 수십 건 × 2 (D+7 + D+30) → DB 부하 미미 | 인덱스 효율 모니터링 (운영 중 지표 확인) |
| 노트 DELETE 시 D+7/D+30 PENDING 행도 cascade 삭제되어야 | 이미 `WriteService.delete` L104 에서 `snapshotRepository.deleteByNoteId(noteId)` 호출 | 기존 cascade 동작 재확인 |
| 기존 데이터 마이그레이션 필요? | 없음 — 본 PR 이 첫 도입, 운영 데이터 0건 | n/a |

## 테스트 가능성

도메인 메서드 변경 없음. WriteService.create 의 saveAll 호출 횟수/인자 검증은 단위 테스트(향후 명시 요청 시)로 가능.

## 승인 대기

태형님 승인 후 구현 진행.