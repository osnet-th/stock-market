package com.thlee.stock.market.stockmarket.notification.infrastructure.email;

import com.thlee.stock.market.stockmarket.notification.domain.NotificationRequest;
import com.thlee.stock.market.stockmarket.notification.domain.NotificationService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * 이메일 알림 발송 구현체
 */
@Slf4j
@Component
public class EmailNotificationService implements NotificationService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailNotificationService(JavaMailSender mailSender,
                                    @Value("${spring.mail.username:}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendNotification(NotificationRequest request) {
        if (request.getRecipientEmail() == null || request.getRecipientEmail().isBlank()) {
            log.warn("이메일 주소가 없어 발송을 건너뜁니다. userId={}", request.getUserId());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            if (fromAddress != null && !fromAddress.isBlank()) {
                helper.setFrom(fromAddress);
            }
            helper.setTo(request.getRecipientEmail());
            helper.setSubject(request.getTitle());
            helper.setText(request.getMessage(), true);

            mailSender.send(message);
            log.info("이메일 발송 완료: userId={}, to={}", request.getUserId(), request.getRecipientEmail());
        } catch (MessagingException e) {
            log.error("이메일 발송 실패: userId={}, to={}", request.getUserId(), request.getRecipientEmail(), e);
            throw new RuntimeException("이메일 발송에 실패했습니다.", e);
        }
    }
}