package com.thlee.stock.market.stockmarket.notification.domain;

/**
 * 알림 발송 요청 객체
 */
public class NotificationRequest {
    private final Long userId;
    private final String title;
    private final String message;

    public NotificationRequest(Long userId, String title, String message) {
        this.userId = userId;
        this.title = title;
        this.message = message;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }
}