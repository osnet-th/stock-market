package com.thlee.stock.market.stockmarket.realestate.application;

import com.thlee.stock.market.stockmarket.realestate.infrastructure.config.RealEstateMarketAsyncConfig;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.scheduler.RealEstateMarketBatchScheduler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 부동산 일배치 수동 트리거 유스케이스.
 * <p>
 * Controller(presentation)는 본 service 만 의존하고, infrastructure 의 BatchScheduler 는 service 가 호출.
 * 의존성 방향(presentation → application → infrastructure) 정합.
 * <p>
 * 응답은 즉시 ack 만 반환하며 실제 fetch 는 admin executor 에서 비동기 실행.
 * 동시 호출은 in-flight guard 로 차단(409 rejected).
 */
@Service
@Slf4j
public class RealEstateBatchTriggerService {

    private final RealEstateMarketBatchScheduler batchScheduler;
    private final ThreadPoolTaskExecutor adminBatchExecutor;
    private final AtomicBoolean inFlight = new AtomicBoolean(false);

    public RealEstateBatchTriggerService(
            RealEstateMarketBatchScheduler batchScheduler,
            @Qualifier(RealEstateMarketAsyncConfig.ADMIN_EXECUTOR_BEAN) ThreadPoolTaskExecutor adminBatchExecutor) {
        this.batchScheduler = batchScheduler;
        this.adminBatchExecutor = adminBatchExecutor;
    }

    public TriggerResult trigger() {
        if (!inFlight.compareAndSet(false, true)) {
            log.info("[realestate.admin] trigger rejected — another batch in flight");
            return TriggerResult.rejected("another batch in flight");
        }
        adminBatchExecutor.execute(() -> {
            try {
                batchScheduler.runDailyBatch();
            } finally {
                inFlight.set(false);
            }
        });
        return TriggerResult.triggered();
    }

    public boolean isInFlight() {
        return inFlight.get();
    }

    public record TriggerResult(String status, String detail, String triggeredAt) {

        static TriggerResult triggered() {
            return new TriggerResult("triggered", null, LocalDateTime.now().toString());
        }

        static TriggerResult rejected(String reason) {
            return new TriggerResult("rejected", reason, LocalDateTime.now().toString());
        }
    }
}