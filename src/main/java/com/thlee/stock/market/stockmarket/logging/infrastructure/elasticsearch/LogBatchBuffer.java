package com.thlee.stock.market.stockmarket.logging.infrastructure.elasticsearch;

import com.thlee.stock.market.stockmarket.logging.domain.model.ApplicationLog;
import com.thlee.stock.market.stockmarket.logging.infrastructure.async.LogAsyncConfig;
import com.thlee.stock.market.stockmarket.logging.infrastructure.elasticsearch.document.ApplicationLogDocument;
import com.thlee.stock.market.stockmarket.logging.infrastructure.elasticsearch.mapper.LogDocumentMapper;
import io.micrometer.core.instrument.Counter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 로그 적재 배치 버퍼.
 *
 * 세 조건 중 하나라도 충족 시 flush:
 * <ul>
 *   <li>5초 주기 (전용 flush 스레드)</li>
 *   <li>500건 누적</li>
 *   <li>5MB 누적</li>
 * </ul>
 *
 * flush 시 월별 인덱스({@code app-audit-YYYY.MM} 등) 기준으로 그룹화하여 인덱스당 한 번의
 * {@code bulkIndex} 호출로 전송한다.
 *
 * ES 호출은 전용 단일 스레드({@code log-flush})에서만 실행한다 (#96).
 * ES 클라이언트의 I/O reactor 사망 시 bulk 호출이 무기한 블로킹될 수 있는데,
 * 공용 스케줄러 풀이나 호출자 스레드가 여기에 잠식되면 cron 배치 전체가 정지하므로
 * flush 실행 주체를 이 스레드 하나로 격리한다. flush 스레드가 행 상태로 버퍼가
 * 하드캡(1,000건/10MB)에 도달하면 신규 로그를 드롭하고 카운터로 계수한다.
 *
 * 그레이스풀 셧다운: {@link #drainOnShutdown} 가 남은 버퍼를 flush — 유실 최소화.
 * 모든 flush 실패는 삼키고 WARN 로그만 남긴다 (best-effort at-most-once).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogBatchBuffer {

    private static final int MAX_ITEMS = 500;
    private static final int MAX_BYTES = 5 * 1024 * 1024;    // 5MB
    private static final int HARD_CAP_ITEMS = MAX_ITEMS * 2;    // flush 스레드 행 시 드롭 기준
    private static final int HARD_CAP_BYTES = MAX_BYTES * 2;
    private static final long FLUSH_INTERVAL_MS = 5_000L;
    private static final Duration SHUTDOWN_DRAIN_TIMEOUT = Duration.ofSeconds(10);
    private static final long SHUTDOWN_POLL_INTERVAL_MS = 50L;

    private final ElasticsearchOperations elasticsearchOperations;
    private final LogMonthlyIndexNameResolver indexNameResolver;

    @Qualifier(LogAsyncConfig.DROPPED_COUNTER_BEAN_NAME)
    private final Counter logIngestionDroppedCounter;

    private final Object lock = new Object();
    private List<ApplicationLog> buffer = new ArrayList<>();
    private int bufferedBytes = 0;

    private ScheduledExecutorService flushExecutor;
    private final AtomicBoolean flushRequested = new AtomicBoolean(false);
    private boolean dropWarned = false;    // lock 보호 — 행 에피소드당 1회만 WARN

    @PostConstruct
    void startFlushExecutor() {
        flushExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "log-flush");
            thread.setDaemon(true);
            return thread;
        });
        flushExecutor.scheduleWithFixedDelay(
                this::flushPending, FLUSH_INTERVAL_MS, FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    public void enqueue(ApplicationLog log) {
        if (log == null) {
            return;
        }
        boolean flushNeeded = false;
        synchronized (lock) {
            if (dropIfHardCapReached()) {
                return;
            }
            buffer.add(log);
            bufferedBytes += estimateBytes(log);
            if (buffer.size() >= MAX_ITEMS || bufferedBytes >= MAX_BYTES) {
                flushNeeded = true;
            }
        }
        if (flushNeeded) {
            requestFlush();
        }
    }

    /** lock 보유 상태에서만 호출한다. 하드캡 도달 시 드롭 계수 후 true 를 반환한다. */
    private boolean dropIfHardCapReached() {
        if (buffer.size() < HARD_CAP_ITEMS && bufferedBytes < HARD_CAP_BYTES) {
            return false;
        }
        logIngestionDroppedCounter.increment();
        if (!dropWarned) {
            dropWarned = true;
            log.warn("로그 버퍼 하드캡 도달 ({}건/{}bytes) — flush 미진행(ES 행 의심), 신규 로그 드롭 시작",
                    buffer.size(), bufferedBytes);
        }
        return true;
    }

    /**
     * 임계 초과 시 전용 스레드에 flush 를 요청한다. 호출자 스레드는 ES 에 직접 붙지 않는다 (#96).
     * 이미 요청이 걸려 있으면 중복 제출하지 않는다.
     */
    private void requestFlush() {
        if (flushRequested.compareAndSet(false, true)) {
            try {
                flushExecutor.execute(this::flushPending);
            } catch (RejectedExecutionException e) {
                // 셧다운 진행 중 — drainOnShutdown 이 잔여분을 처리한다
                flushRequested.set(false);
            }
        }
    }

    /**
     * 전용 flush 스레드에서만 실행된다 (주기 + 임계 초과 요청).
     * 예외가 새어나가면 {@code scheduleWithFixedDelay} 주기 실행이 조용히 영구 취소되므로
     * Error 를 포함해 전부 방어한다 (#96 리뷰 M2).
     */
    private void flushPending() {
        try {
            flushRequested.set(false);
            List<ApplicationLog> toFlush;
            synchronized (lock) {
                if (buffer.isEmpty()) {
                    return;
                }
                toFlush = swapOut();
                dropWarned = false;
            }
            flush(toFlush);
        } catch (Throwable t) {
            log.warn("로그 flush 실행 실패 (주기 유지)", t);
        }
    }

    /**
     * 앱 종료 시 잔여 버퍼 flush. @PreDestroy 훅.
     *
     * drain 도 전용 flush 스레드에 제출하고 최대 {@link #SHUTDOWN_DRAIN_TIMEOUT} 만 대기한다.
     * flush 스레드가 행 상태(ES 무응답)면 타임아웃 후 잔여 건 유실을 감수하고 즉시 종료해
     * 앱 셧다운이 지연되지 않도록 한다 (best-effort at-most-once, #96).
     */
    @PreDestroy
    public void drainOnShutdown() {
        Future<?> drain;
        try {
            drain = flushExecutor.submit(this::drainLoop);
        } catch (RejectedExecutionException e) {
            log.warn("LogBatchBuffer drain 제출 실패(executor 종료됨), 잔여 로그 유실 감수");
            return;
        }

        try {
            drain.get(SHUTDOWN_DRAIN_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.warn("LogBatchBuffer drain 타임아웃({}s): flush 스레드 미응답, 잔여 {}건 유실 감수",
                    SHUTDOWN_DRAIN_TIMEOUT.toSeconds(), bufferedCount());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("LogBatchBuffer drain 인터럽트, 즉시 종료");
        } catch (ExecutionException e) {
            log.warn("LogBatchBuffer drain 실행 실패: {}", e.getMessage());
        } finally {
            flushExecutor.shutdownNow();
        }
    }

    /** 전용 flush 스레드에서 실행 — 잔여 버퍼를 반복 drain (외부에서 유한 대기로 제한). */
    private void drainLoop() {
        int totalFlushed = 0;

        while (!Thread.currentThread().isInterrupted()) {
            List<ApplicationLog> toFlush;
            synchronized (lock) {
                if (buffer.isEmpty()) {
                    break;
                }
                toFlush = swapOut();
            }
            totalFlushed += toFlush.size();
            log.info("LogBatchBuffer shutdown drain 진행: 이번 {}건 (누적 {}건)",
                    toFlush.size(), totalFlushed);
            flush(toFlush);

            // 진행 중 async 리스너가 뒤이어 enqueue 할 수 있도록 짧게 양보
            try {
                Thread.sleep(SHUTDOWN_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("LogBatchBuffer drain 인터럽트, 즉시 종료 (누적 {}건 flush 완료)", totalFlushed);
                return;
            }
        }

        if (totalFlushed > 0) {
            log.info("LogBatchBuffer shutdown drain 완료: 누적 {}건", totalFlushed);
        }
    }

    private int bufferedCount() {
        synchronized (lock) {
            return buffer.size();
        }
    }

    private List<ApplicationLog> swapOut() {
        List<ApplicationLog> out = buffer;
        buffer = new ArrayList<>();
        bufferedBytes = 0;
        return out;
    }

    private void flush(List<ApplicationLog> logs) {
        Map<String, List<IndexQuery>> grouped = new LinkedHashMap<>();
        for (ApplicationLog log : logs) {
            try {
                ApplicationLogDocument doc = LogDocumentMapper.toDocument(log);
                String indexName = indexNameResolver.resolve(log);
                IndexQuery query = new IndexQueryBuilder()
                        .withId(doc.getId())
                        .withObject(doc)
                        .build();
                grouped.computeIfAbsent(indexName, k -> new ArrayList<>()).add(query);
            } catch (Exception e) {
                logIngestionDroppedCounter.increment();
                LogBatchBuffer.log.warn("로그 문서 매핑 실패, 드롭: requestId={}, err={}",
                        log.requestId(), e.getMessage());
            }
        }
        for (Map.Entry<String, List<IndexQuery>> entry : grouped.entrySet()) {
            try {
                elasticsearchOperations.bulkIndex(entry.getValue(), IndexCoordinates.of(entry.getKey()));
            } catch (Exception e) {
                logIngestionDroppedCounter.increment(entry.getValue().size());
                LogBatchBuffer.log.warn("ES bulkIndex 실패 (index={}, 건수={}): {}",
                        entry.getKey(), entry.getValue().size(), e.getMessage());
                // 인터럽트 유래 예외가 여기서 삼켜지면 drainLoop 의 종료 가드가 무력화되므로 복원 (#96 리뷰 L1)
                if (causedByInterrupt(e)) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private static boolean causedByInterrupt(Throwable t) {
        for (Throwable cause = t; cause != null; cause = cause.getCause()) {
            if (cause instanceof InterruptedException) {
                return true;
            }
        }
        return false;
    }

    private int estimateBytes(ApplicationLog log) {
        int total = 64; // overhead 대략치
        if (log.requestId() != null) total += log.requestId().length();
        if (log.payload() != null) {
            for (Map.Entry<String, Object> entry : log.payload().entrySet()) {
                total += entry.getKey().length();
                total += valueBytes(entry.getValue());
            }
        }
        return total;
    }

    private int valueBytes(Object v) {
        if (v == null) return 4;
        if (v instanceof String s) return s.length();
        if (v instanceof Number || v instanceof Boolean) return 16;
        return v.toString().length();
    }
}
