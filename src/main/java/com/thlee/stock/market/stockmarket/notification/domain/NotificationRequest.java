package com.thlee.stock.market.stockmarket.notification.domain;

import lombok.Getter;

/**
 * 알림 발송 요청 객체
 */
@Getter
public class NotificationRequest {
    private final Long userId;
    private final String recipientEmail;
    private final String title;
    private final String message;

    public NotificationRequest(Long userId, String recipientEmail, String title, String message) {
        if (recipientEmail != null && (recipientEmail.contains("\r") || recipientEmail.contains("\n"))) {
            throw new IllegalArgumentException("이메일 주소에 유효하지 않은 문자가 포함되어 있습니다.");
        }
        this.userId = userId;
        this.recipientEmail = recipientEmail;
        this.title = title;
        this.message = message;
    }
}