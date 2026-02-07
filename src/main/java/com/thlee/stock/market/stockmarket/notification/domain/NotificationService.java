package com.thlee.stock.market.stockmarket.notification.domain;

/**
 * 알림 발송 서비스 인터페이스
 * 이메일, 카카오톡 등 다양한 알림 채널 구현체를 추상화
 */
public interface NotificationService {
    /**
     * 알림 발송
     *
     * @param request 알림 요청 (userId, title, message)
     */
    void sendNotification(NotificationRequest request);
}