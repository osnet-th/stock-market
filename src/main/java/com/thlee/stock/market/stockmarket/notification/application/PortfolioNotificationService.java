package com.thlee.stock.market.stockmarket.notification.application;

import com.thlee.stock.market.stockmarket.notification.domain.NotificationRequest;
import com.thlee.stock.market.stockmarket.notification.domain.NotificationService;
import com.thlee.stock.market.stockmarket.portfolio.application.PortfolioEvaluationService;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioEvaluation;
import com.thlee.stock.market.stockmarket.user.domain.model.OAuthAccount;
import com.thlee.stock.market.stockmarket.user.domain.model.User;
import com.thlee.stock.market.stockmarket.user.domain.repository.OAuthAccountRepository;
import com.thlee.stock.market.stockmarket.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 포트폴리오 마감 메일 알림 오케스트레이션 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioNotificationService {

    private final UserRepository userRepository;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final PortfolioEvaluationService portfolioEvaluationService;
    private final ReportRenderer reportRenderer;
    private final NotificationService notificationService;

    /**
     * 알림 활성화 사용자에게 포트폴리오 마감 리포트 발송
     * @return 발송 성공 건수
     */
    public int sendMarketCloseNotifications() {
        // 1. 알림 대상 사용자 조회
        List<User> users = userRepository.findByNotificationEnabled(true);
        if (users.isEmpty()) {
            log.info("알림 대상 사용자가 없습니다.");
            return 0;
        }

        log.info("알림 대상 사용자: {}명", users.size());

        // 2. 사용자별 이메일 조회
        Map<Long, String> userEmailMap = resolveUserEmails(users);

        // 3. 포트폴리오 일괄 평가 (고유 종목 1회 조회)
        List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());
        Map<Long, PortfolioEvaluation> evaluations = portfolioEvaluationService.evaluatePortfolios(userIds);

        // 4. 사용자별 메일 발송 (개별 try-catch로 격리)
        LocalDate today = LocalDate.now();
        String subject = "[Stock Market] 포트폴리오 마감 리포트 (" + today.format(DateTimeFormatter.ISO_LOCAL_DATE) + ")";
        int successCount = 0;

        for (User user : users) {
            try {
                String email = userEmailMap.get(user.getId());
                if (email == null) {
                    log.warn("이메일 없음, 발송 건너뜀: userId={}", user.getId());
                    continue;
                }

                PortfolioEvaluation evaluation = evaluations.get(user.getId());
                if (evaluation == null || evaluation.getItems().isEmpty()) {
                    log.info("포트폴리오 없음, 발송 건너뜀: userId={}", user.getId());
                    continue;
                }

                String htmlContent = reportRenderer.renderReport(evaluation, today);

                NotificationRequest request = new NotificationRequest(
                        user.getId(), email, subject, htmlContent);
                notificationService.sendNotification(request);
                successCount++;
            } catch (Exception e) {
                log.error("메일 발송 실패: userId={}", user.getId(), e);
            }
        }

        log.info("메일 발송 완료: 성공={}/전체={}", successCount, users.size());
        return successCount;
    }

    private Map<Long, String> resolveUserEmails(List<User> users) {
        return users.stream()
                .collect(Collectors.toMap(
                        User::getId,
                        user -> {
                            List<OAuthAccount> accounts = oAuthAccountRepository.findByUserId(user.getId());
                            return accounts.stream()
                                    .map(OAuthAccount::getEmail)
                                    .filter(email -> email != null && !email.isBlank())
                                    .findFirst()
                                    .orElse(null);
                        },
                        (existing, replacement) -> existing
                ));
    }
}