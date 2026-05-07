package com.thlee.stock.market.stockmarket.realestate.presentation;

import com.thlee.stock.market.stockmarket.realestate.infrastructure.scheduler.RealEstateMarketBatchScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 운영자 전용 부동산 도메인 엔드포인트.
 * <p>
 * {@code /api/admin/realestate/**} — {@code AdminGuardInterceptor} 가
 * userId 화이트리스트로 가드. cron 도래를 기다리지 않고 즉시 일배치를
 * 트리거할 수 있다 (스모크 검증/긴급 갱신 용도).
 */
@RestController
@RequestMapping("/api/admin/realestate")
@RequiredArgsConstructor
@Slf4j
public class RealEstateAdminController {

    private final RealEstateMarketBatchScheduler batchScheduler;

    /**
     * 부동산 일배치 즉시 트리거. 응답은 트리거 ack만 반환하며 실제 fetch는
     * realEstateFetchExecutor 풀에서 비동기로 진행된다.
     * 인증키 미설정 출처는 IllegalStateException → 항목 단위 skip.
     */
    @PostMapping("/batch/run")
    public ResponseEntity<Map<String, Object>> runBatch() {
        log.info("[realestate.admin] manual batch trigger");
        batchScheduler.runDailyBatch();
        return ResponseEntity.ok(Map.of(
                "status", "completed",
                "triggeredAt", LocalDateTime.now().toString()
        ));
    }
}