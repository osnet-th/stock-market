package com.thlee.stock.market.stockmarket.notification.infrastructure.email;

import com.thlee.stock.market.stockmarket.notification.domain.NotificationRequest;
import com.thlee.stock.market.stockmarket.notification.domain.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * 이메일 알림 발송 구현체
 */
@Component
@RequiredArgsConstructor
public class EmailNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    @Override
    public void sendNotification(NotificationRequest request) {
    }
}