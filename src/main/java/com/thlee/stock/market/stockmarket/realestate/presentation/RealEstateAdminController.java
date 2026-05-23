package com.thlee.stock.market.stockmarket.realestate.presentation;

import com.thlee.stock.market.stockmarket.realestate.application.RealEstateBatchTriggerService;
import com.thlee.stock.market.stockmarket.realestate.application.RealEstateBatchTriggerService.TriggerResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 운영자 전용 부동산 도메인 엔드포인트.
 * <p>
 * {@code /api/admin/realestate/**} — {@code AdminGuardInterceptor} 가 userId 화이트리스트로 가드.
 * 응답은 즉시 ack 반환 (admin executor 에서 비동기 dispatch). 동시 호출은 409.
 */
@RestController
@RequestMapping("/api/admin/realestate")
@RequiredArgsConstructor
@Slf4j
public class RealEstateAdminController {

    private final RealEstateBatchTriggerService triggerService;

    @PostMapping("/batch/run")
    public ResponseEntity<TriggerResult> runBatch() {
        log.info("[realestate.admin] manual batch trigger request received");
        TriggerResult result = triggerService.trigger();
        if ("rejected".equals(result.status())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }
        return ResponseEntity.accepted().body(result);  // 202 Accepted: 비동기 처리 시작
    }
}
