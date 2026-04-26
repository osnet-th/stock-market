package com.thlee.stock.market.stockmarket.stocknote.application.listener;

import com.thlee.stock.market.stockmarket.stocknote.application.StockNoteSnapshotCaptureExecutor;
import com.thlee.stock.market.stockmarket.stocknote.application.event.StockNoteCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * {@link StockNoteCreatedEvent} 를 수신해 AT_NOTE 스냅샷을 비동기로 캡처한다.
 *
 * <p>self-invocation 회피를 위해 {@link StockNoteSnapshotCaptureExecutor} 의 자기 트랜잭션을 활용.
 * {@code AFTER_COMMIT} 단계로 묶어 기록 생성 트랜잭션이 롤백될 경우 스냅샷 호출 자체가
 * 발생하지 않도록 한다 (async 리서치 심화 6).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockNoteCreatedSnapshotListener {

    private final StockNoteSnapshotCaptureExecutor captureExecutor;

    @Async("stocknoteSnapshotExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStockNoteCreated(StockNoteCreatedEvent event) {
        try {
            captureExecutor.captureAtNote(event.noteId());
        } catch (Exception e) {
            // captureAtNote 내부에서 FAILED 로 저장되지만, 혹시 상위 예외가 새는 경우 대비
            log.error("AT_NOTE async dispatch error: noteId={}, reason={}",
                    event.noteId(), e.getMessage(), e);
        }
    }
}
