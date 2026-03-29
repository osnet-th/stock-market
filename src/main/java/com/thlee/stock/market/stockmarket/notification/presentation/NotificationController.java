package com.thlee.stock.market.stockmarket.notification.presentation;

import com.thlee.stock.market.stockmarket.notification.application.PortfolioNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 알림 테스트용 엔드포인트 (개발/테스트 환경 전용)
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final PortfolioNotificationService portfolioNotificationService;

    /**
     * 포트폴리오 마감 알림 수동 발송 테스트
     * 스케줄러와 동일한 로직을 즉시 실행
     */
    @PostMapping("/test/market-close")
    public ResponseEntity<Map<String, Object>> testMarketCloseNotification() {
        int successCount = portfolioNotificationService.sendMarketCloseNotifications();
        return ResponseEntity.ok(Map.of(
                "message", "마감 알림 발송 완료",
                "successCount", successCount
        ));
    }
}