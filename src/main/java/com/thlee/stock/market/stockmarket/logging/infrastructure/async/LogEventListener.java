package com.thlee.stock.market.stockmarket.logging.infrastructure.async;

import com.thlee.stock.market.stockmarket.logging.application.event.ApplicationLogEvent;
import com.thlee.stock.market.stockmarket.logging.domain.model.ApplicationLog;
import com.thlee.stock.market.stockmarket.logging.domain.model.LogDomain;
import com.thlee.stock.market.stockmarket.logging.domain.service.LogIndexPort;
import com.thlee.stock.market.stockmarket.logging.domain.service.LogSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link ApplicationLogEvent} 비동기 처리 리스너.
 *
 * 도메인별 디스패치 전략:
 * <ul>
 *   <li>AUDIT/ERROR — 즉시 처리. 트랜잭션 롤백 여부와 무관하게 "요청이 있었다"는 사실 기록</li>
 *   <li>BUSINESS — {@code AFTER_COMMIT} 에서만 처리. 롤백된 트랜잭션의 orphan 로그 방지</li>
 * </ul>
 *
 * ES 장애/sanitize 실패는 모두 삼키고 drop counter 만 증가시킨다 (best-effort at-most-once).
 * sanitize 자체 실패 시 원본 대신 최소 fallback 문서를 저장해 requestId 단절을 막는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogEventListener {

    private final LogIndexPort logIndexPort;
    private final LogSanitizer logSanitizer;

    @Async(LogAsyncConfig.EXECUTOR_BEAN_NAME)
    @EventListener
    public void onImmediate(ApplicationLogEvent event) {
        if (event.domain() == LogDomain.BUSINESS) {
            return;  // AFTER_COMMIT 리스너에서 처리
        }
        safeSave(event);
    }

    @Async(LogAsyncConfig.EXECUTOR_BEAN_NAME)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAfterCommit(ApplicationLogEvent event) {
        if (event.domain() != LogDomain.BUSINESS) {
            return;
        }
        safeSave(event);
    }

    private void safeSave(ApplicationLogEvent event) {
        ApplicationLog toSave;
        try {
            toSave = logSanitizer.sanitize(event.toApplicationLog());
        } catch (Exception e) {
            log.warn("Log sanitize 실패, fallback 문서 저장 시도. requestId={}, domain={}, err={}",
                    event.requestId(), event.domain(), e.getMessage());
            toSave = fallback(event, e);
        }
        try {
            logIndexPort.save(toSave);
        } catch (Exception e) {
            log.warn("Log 인덱싱 실패, drop. requestId={}, domain={}, err={}",
                    event.requestId(), event.domain(), e.getMessage());
        }
    }

    private ApplicationLog fallback(ApplicationLogEvent event, Exception sanitizeFailure) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("_sanitize_failed", true);
        payload.put("_error", sanitizeFailure.getClass().getSimpleName());
        return new ApplicationLog(
                event.timestamp(),
                event.domain(),
                event.userId(),
                event.requestId(),
                payload,
                false,
                null
        );
    }
}