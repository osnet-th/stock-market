# [stocknote] AT_NOTE FAILED 시 D+N changePercent null 영구화

> ce-review 2026-04-25 P1 #6 (adversarial + reliability 합의). plan task: Phase 10 P1.

## 현재 상태

### 시나리오

1. 사용자가 노트 생성 → AT_NOTE PENDING + D+7 PENDING + D+30 PENDING (Task #1 완료 상태)
2. AsyncDispatcher 가 captureAtNote 호출 → KIS 일시 장애 → markFailed → retryCount=1, 2, 3 → AT_NOTE 영구 FAILED
3. D+7 도달일 cron → captureForMarket → captureTarget 호출
4. `findDueForCapture` 의 LEFT JOIN 결과 `atps.status='SUCCESS'` 조건 → AT_NOTE 가 SUCCESS 가 아니면 `at_note_close_price=NULL`
5. captureTarget 가 `markSuccess(today, currentPrice, null)` 호출
6. `computeChangePercent(null, current)` → null 반환
7. status=SUCCESS, **changePercent=null 영구 저장**

### 코드 위치

`StockNoteSnapshotCaptureExecutor.java:101`
```java
snapshot.markSuccess(LocalDate.now(), price, target.atNoteClosePrice());
```

`StockNotePriceSnapshot.java:115-122` `computeChangePercent`
```java
if (base == null || base.signum() == 0 || current == null) {
    return null;
}
```

`StockNotePriceSnapshotJpaRepository.java:64-67` `findDueForCapture` LEFT JOIN
```sql
LEFT JOIN stock_note_price_snapshot atps
       ON atps.note_id = n.id
      AND atps.snapshot_type = 'AT_NOTE'
      AND atps.status = 'SUCCESS'   -- ← AT_NOTE 가 SUCCESS 가 아니면 NULL
```

### 영향 범위

- 차트 scatter `priceAtNote` 가 AT_NOTE.closePrice 에 의존(`StockNoteChartService.java:74-83`) — AT_NOTE FAILED 노트는 차트에서 사라짐
- D+7/D+30 SUCCESS + changePercent=null → "성공인데 변화율 N/A" 라는 모순된 상태가 검증 패널에 노출
- 대시보드 `hitRate` / 유사 패턴 `avgD7Percent`/`avgD30Percent` 가 잘못된 집계
- AT_NOTE 가 manualRetry 로 늦게 SUCCESS 되어도 D+N changePercent 자동 보강 없음 → 영구 null

## 해결 옵션

### 옵션 A — atNoteClose=null 일 때 D+N 캡처 스킵 (PENDING 유지 + retry 미차감)

`captureTarget` 진입 시점에 `target.atNoteClosePrice() == null` 이면 markSuccess 호출 안 하고 PENDING 유지. AT_NOTE 가 늦게 SUCCESS 되면 다음 회차 cron 에서 D+N 재시도 (Task #24 의 captureForMarket 의 `due.isBefore(asOfDate) || isEqual` 완화 필요).

| 장점 | 단점 |
|---|---|
| 신규 changePercent=null SUCCESS 발생 차단 | Task #24 보완 필요 (현재 cron 은 due.isEqual 만 → 다음 회차 매칭 안 됨) |
| markFailed 안 함 — retry 카운트 미증가 | 단독으로는 효과 제한적 |

### 옵션 B — captureAtNote 성공 직후 D+N changePercent 보강

`captureAtNote` 가 markSuccessIfPending 으로 AT_NOTE SUCCESS 전이 직후, 같은 트랜잭션에서 동일 noteId 의 D+7/D+30 SUCCESS+changePercent=null 행을 조회해 `closePrice` 와 새로 얻은 `atNoteClose` 로 changePercent 재계산 후 save.

| 장점 | 단점 |
|---|---|
| 이미 changePercent=null 로 SUCCESS 된 행도 사후 보강 | captureAtNote 트랜잭션 책임 확장 |
| AT_NOTE 가 늦게 SUCCESS 되어도 D+N 자동 보강 | 보강 로직이 도메인 메서드 추가 필요 |

### 옵션 C — A + B 결합 (권장)

옵션 A 로 신규 발생 차단 + 옵션 B 로 사후 보강 + 향후 잔존 case 도 처리.

| 장점 | 단점 |
|---|---|
| 완전한 보호 — 신규 차단 + 잔존 보강 | 변경 범위 약간 증가 |
| 데이터 정합성 회복 | |

## 추천: 옵션 C

근거:
- 옵션 A 만으로는 captureForMarket cron 한 회차 시 AT_NOTE 가 아직 PENDING/FAILED 인 경우 D+N 도 PENDING 잔존 → Task #24 의존 (현재 미해결)
- 옵션 B 만으로는 changePercent=null SUCCESS 가 계속 생성되어 보강 시점까지 stale
- 두 옵션은 서로 영향 없는 독립 변경

## 코드 위치

| 파일 | 변경 |
|---|---|
| `StockNoteSnapshotCaptureExecutor.captureTarget` | atNoteClose=null 시 PENDING 유지 + log.warn |
| `StockNoteSnapshotCaptureExecutor.captureAtNote` | markSuccessIfPending 성공 직후 backfill 호출 |
| `StockNotePriceSnapshot` | `backfillChangePercent(BigDecimal atNotePrice)` 도메인 메서드 추가 |
| `StockNotePriceSnapshotRepository` | (이미 있음) `findAllByNoteIds` 활용 또는 `findByNoteIdAndType` 두 번 |

## 후속 task 와의 관계

| Task | 정합 |
|---|---|
| #24 P2 retryPending 가 D+7/D+30 도 처리 | 본 task 의 옵션 A 가 PENDING 유지하면 #24 후 자동 재시도 가능 |
| #5 P1 BusinessDayCalculator 음력 공휴일 | 이미 완료. 휴장일 captureForMarket 호출 자체가 줄어들어 본 task 발생 빈도 감소 |
| #21 P2 manualRetry self-invocation 트랜잭션 분리 | 본 task 의 captureAtNote backfill 이 같은 트랜잭션에서 동작 — manualRetry outer 트랜잭션과도 정합 |

## 설계 문서

[d-plus-n-change-percent-null](../../../designs/stocknote/d-plus-n-change-percent-null/d-plus-n-change-percent-null.md)
