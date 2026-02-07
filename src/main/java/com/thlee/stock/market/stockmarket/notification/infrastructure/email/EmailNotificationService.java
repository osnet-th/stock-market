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
    private final JavaMailSender mailSender;

    @Override
    public void sendNotification(NotificationRequest request) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo("user-" + request.getUserId() + "@stock-market.com"); // TODO: 실제 사용자 이메일 조회
            message.setSubject(request.getTitle());
            message.setText(request.getMessage());

            mailSender.send(message);
            log.info("이메일 알림 발송 완료. userId: {}, title: {}", request.getUserId(), request.getTitle());
        } catch (Exception e) {
            log.warn("이메일 알림 발송 실패. userId: {}, reason: {}", request.getUserId(), e.getMessage());
        }
    }
}