package com.thlee.stock.market.stockmarket.news.infrastructure.scheduler;

import com.thlee.stock.market.stockmarket.logging.application.LoggingContext;
import com.thlee.stock.market.stockmarket.news.domain.repository.KeywordCollectionHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 수집 이력 보존 기간 정리 (#115).
 *
 * <p>이력은 키워드 수 × 수집 주기만큼 쌓인다 — 키워드 14개에 매시 수집이면 하루 336행,
 * 1년이면 12만 행이다. 화면이 쓰는 건 "마지막 성공"과 "마지막 성공 이후 실패"뿐이라
 * 오래된 행은 가치가 없다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeywordCollectionHistoryCleanupScheduler {

    private final KeywordCollectionHistoryRepository collectionHistoryRepository;

    /** 보존 일수. 연속 실패 판정에 필요한 구간보다 충분히 길게 둔다. */
    @Value("${batch.news.collection-history-retention-days:90}")
    private int retentionDays;

    @Scheduled(cron = "${batch.schedule.news-collection-history-cleanup-cron:0 30 4 * * *}")
    @Transactional
    public void cleanupOldHistories() {
        try (var ctx = LoggingContext.forScheduler("keyword-collection-history-cleanup")) {
            LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);
            int deleted = collectionHistoryRepository.deleteOlderThan(threshold);
            if (deleted > 0) {
                log.info("수집 이력 정리: {}건 삭제 (기준 {})", deleted, threshold);
            }
        }
    }
}
