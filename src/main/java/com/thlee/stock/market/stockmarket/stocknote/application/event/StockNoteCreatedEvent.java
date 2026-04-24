package com.thlee.stock.market.stockmarket.stocknote.application.event;

/**
 * 기록 생성 완료 이벤트.
 *
 * <p>{@code @TransactionalEventListener(phase = AFTER_COMMIT)} + {@code @Async("stocknoteSnapshotExecutor")}
 * 가 부착된 리스너(Phase 3 의 {@code StockNoteSnapshotAsyncDispatcher}) 가 AT_NOTE PENDING 스냅샷을
 * 실제 현재가로 갱신한다. 본 이벤트는 구독자가 생기기 전까지는 no-op.
 */
public record StockNoteCreatedEvent(Long noteId) { }