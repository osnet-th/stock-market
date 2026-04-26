# [stocknote] D+7/D+30 PENDING 행 미생성 — 자동 캡처 영구 미동작

> ce-review 2026-04-25 P0 #1. plan 핵심 가치(적중률·태그 패턴 분석) 무력화 상태.

## 현재 상태

### 기록 생성 시 — AT_NOTE PENDING 만 insert

`StockNoteWriteService.java:69`

```java
snapshotRepository.save(StockNotePriceSnapshot.createPending(noteId, SnapshotType.AT_NOTE));
```

D+7 / D+30 PENDING 은 **어디에서도 insert 되지 않음**. 전체 stocknote 패키지에서 `createPending` 호출 grep 결과 단 1건 (위 라인).

### 스케줄러는 PENDING 행만 스캔

`StockNotePriceSnapshotJpaRepository.java:56-72` `findDueForCapture`

```sql
WHERE ps.snapshot_type = :type
  AND ps.status = 'PENDING'
  AND n.market_type = :marketType
```

D_PLUS_7 / D_PLUS_30 PENDING 행이 존재한 적이 없으므로 **항상 빈 결과**.

### 수동 재시도도 차단

`StockNoteSnapshotService.java:115-116` `manualRetry`

```java
StockNotePriceSnapshot snapshot = snapshotRepository.findByNoteIdAndType(noteId, type)
        .orElseThrow(() -> new IllegalArgumentException("snapshot not found: " + type));
```

D+7/D+30 행 없음 → IllegalArgumentException → 400. 사용자 수동 복구도 막힘.

## 영향 범위

| 기능 | 동작 |
|---|---|
| 스케줄러 자동 D+7/D+30 캡처 | ❌ 영구 미동작 |
| 사용자 수동 D+7/D+30 재시도 | ❌ 400 응답 |
| 차트 scatter `priceAtNote` 표시 | AT_NOTE 만 영향 받지 않음, D+N 결과 비어있음 |
| 대시보드 hitRate 집계 | D+N changePercent 가 항상 null → 적중률 분석 불가 |
| 유사 패턴 매칭 aggregate | avgD7Percent / avgD30Percent 가 항상 null |

## 근본 원인

설계 시점의 의도(plan L432-440): "D+7/D+30 도달일에 스케줄러가 캡처 후 **UPSERT** 로 행 생성".

**구현은 UPDATE 경로만 채택** — `markSuccess` + `markFailed` 모두 기존 행을 전제로 동작. INSERT 분기가 어느 곳에도 없음.

## 코드 위치

| 파일 | 라인 | 역할 |
|---|---|---|
| `StockNoteWriteService.java` | 69 | AT_NOTE PENDING 만 insert |
| `StockNotePriceSnapshotJpaRepository.java` | 56-72 | findDueForCapture 가 PENDING 행 전제 |
| `StockNoteSnapshotService.java` | 89-102 | captureForMarket 이 PendingCaptureRow 기반 |
| `StockNoteSnapshotService.java` | 111-131 | manualRetry 가 행 존재 전제 |
| `StockNotePriceSnapshotEntity.java` | 25-30 | unique (note_id, snapshot_type) 제약 — 동일 type 중복 불가 |

## 해결 옵션

### 옵션 A — WriteService.create 에서 D+7/D+30 도 PENDING insert (권장)

**변경**: `WriteService.create` 가 노트 생성 시 AT_NOTE + D+7 + D+30 = 3행 PENDING 동시 insert.

| 장점 | 단점 |
|---|---|
| 단순 — 변경 범위 1 메서드 + saveAll 1줄 | 노트당 3행 즉시 점유 (D+30 SUCCESS 까지 ~30 영업일 PENDING 잔존) |
| 기존 markSuccessIfPending / markFailed / markSuccess 흐름 그대로 활용 | 인덱스 영향 미미 (사용자당 월 수십 건 규모) |
| manualRetry 의 D+N 경로 즉시 정상 동작 | |
| Native SQL / UPSERT 불필요 | |
| plan UPSERT 의도 충족 (생성 시점에 미리 행 만들고 도달 시점 UPDATE) | |

### 옵션 B — findDueForCapture 가 stock_note 기준 LEFT JOIN + INSERT (UPSERT)

**변경**: 스케줄러가 매 회차 stock_note 본체 기준으로 노트를 찾고, 해당 (noteId, type) 행이 없으면 INSERT.

| 장점 | 단점 |
|---|---|
| PENDING 행 사전 점유 없음 | findDueForCapture + captureTarget 책임 비대화 (조회 + INSERT) |
| 데이터 깨끗 | `INSERT ... ON CONFLICT` PostgreSQL 종속 SQL |
| | manualRetry 경로는 별도로 INSERT 보강 필요 (사용자가 retry 누른 시점에 행 없으면 생성?) |
| | 트랜잭션 경계 복잡 |

## 추천: 옵션 A

근거:
- 변경 범위 최소 (단일 메서드 + 3줄)
- plan acceptance L737-740 ("D+7/D+30: 기록일 + N 영업일에 해당하는 기록을 스케줄러가 배치로 스냅샷") 와 정확히 일치
- D+N PENDING 잔존 비용 < INSERT 책임 분산 비용
- 후속 task #6 (AT_NOTE FAILED 시 D+N changePercent null) 와 task #24 (retryPending 가 D+N 도 처리) 모두 D+N 행 존재 전제 → 옵션 A 채택 시 자연 정합

## 마이그레이션 고려

**불필요** — 본 PR(`feat/stocknote`) 이 stocknote 도메인 첫 도입. 운영 환경에 기존 노트 0건. 기존 노트 보강 스크립트 필요 없음.

## 설계 문서

[d-plus-n-pending-fix](../../../designs/stocknote/d-plus-n-pending-fix/d-plus-n-pending-fix.md)