package com.thlee.stock.market.stockmarket.keyword.infrastructure.scheduler;

import com.thlee.stock.market.stockmarket.keyword.application.KeywordService;
import com.thlee.stock.market.stockmarket.keyword.domain.model.Keyword;
import com.thlee.stock.market.stockmarket.news.application.NewsApiFallbackService;
import com.thlee.stock.market.stockmarket.news.domain.model.News;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsSearchResult;
import com.thlee.stock.market.stockmarket.news.domain.repository.NewsRepository;
import com.thlee.stock.market.stockmarket.news.domain.service.NewsDomainService;
import com.thlee.stock.market.stockmarket.notification.domain.NotificationRequest;
import com.thlee.stock.market.stockmarket.notification.domain.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 키워드 뉴스 스케줄러
 * 활성화된 키워드별로 매 시간 뉴스 검색 → 자동 저장 → 알림 발송
 */
@Component
@RequiredArgsConstructor
public class KeywordNewsScheduler {

    private static final Logger log = LoggerFactory.getLogger(KeywordNewsScheduler.class);

    private final KeywordService keywordService;
    private final NewsApiFallbackService newsApiFallbackService;
    private final NewsDomainService newsDomainService;
    private final NewsRepository newsRepository;
    private final NotificationService notificationService;

    /**
     * 매 시간 키워드 뉴스 검색 실행
     * 동일 키워드를 등록한 사용자가 여러 명이면 API 호출은 키워드당 1회
     */
    @Scheduled(cron = "0 0 * * * *")
    public void searchKeywordNews() {
        List<Keyword> activeKeywords = keywordService.getAllActiveKeywords();
        if (activeKeywords.isEmpty()) {
            return;
        }

        // 키워드별로 그룹화 (동일 키워드 → API 호출 1회)
        Map<String, List<Keyword>> keywordGroups = activeKeywords.stream()
                .collect(Collectors.groupingBy(Keyword::getKeyword));

        LocalDateTime from = LocalDateTime.now().minusHours(1);

        for (Map.Entry<String, List<Keyword>> entry : keywordGroups.entrySet()) {
            String keyword = entry.getKey();
            List<Keyword> keywords = entry.getValue();

            // API 호출 (키워드당 1회, 폴백 전략 적용)
            List<NewsSearchResult> results;
            try {
                results = newsApiFallbackService.searchNews(keyword, from);
            } catch (Exception e) {
                log.warn("키워드 '{}' 뉴스 API 호출 실패: {}", keyword, e.getMessage());
                continue;
            }

            if (results.isEmpty()) {
                continue;
            }

            // 각 사용자별로 중복 체크 + 저장 + 알림
            for (Keyword kw : keywords) {
                processKeywordForUser(kw.getUserId(), results);
            }
        }
    }

    /**
     * 사용자별 중복 체크 → 저장 → 알림
     */
    private void processKeywordForUser(Long userId, List<NewsSearchResult> apiResults) {
        List<String> existingUrls = newsRepository.findUrlsByUserId(userId);

        List<News> newNews = newsDomainService.filterAndConvertToNews(
                apiResults, existingUrls, userId, "AUTO_KEYWORD", null
        );

        if (newNews.isEmpty()) {
            return;
        }

        // 배치 저장
        newsRepository.saveAll(newNews);
        log.info("사용자 {} 키워드 매칭 뉴스 {}건 저장 완료", userId, newNews.size());

        // 알림 발송 (저장과 별도 처리, 실패 시 저장에 영향 없음)
        sendNotifications(userId, newNews);
    }

    /**
     * 저장된 뉴스에 대한 알림 발송
     * 알림 실패 시 로그만 남기고 무시
     */
    private void sendNotifications(Long userId, List<News> savedNews) {
        try {
            for (News news : savedNews) {
                NotificationRequest request = new NotificationRequest(
                        userId,
                        news.getTitle(),
                        "키워드 매칭 뉴스가 등록되었습니다."
                );
                notificationService.sendNotification(request);
            }
        } catch (Exception e) {
            log.warn("사용자 {} 키워드 매칭 뉴스 알림 발송 실패: {}", userId, e.getMessage());
        }
    }
}