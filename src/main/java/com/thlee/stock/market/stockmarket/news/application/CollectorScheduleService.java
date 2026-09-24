package com.thlee.stock.market.stockmarket.news.application;

import com.thlee.stock.market.stockmarket.news.application.dto.CollectorScheduleResponse;
import com.thlee.stock.market.stockmarket.news.domain.repository.KeywordCollectionHistoryRepository;
import com.thlee.stock.market.stockmarket.news.domain.repository.KeywordCollectionSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 수집 스케줄 상태 조회 (#115).
 *
 * <p>다음 실행은 스케줄러와 <b>같은 설정값</b>을 읽어 계산한다 — cron 을 하드코딩하면
 * 설정을 바꿨을 때 화면 표시만 틀어진다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectorScheduleService {

    /** 매시 정각형: 초 0, 분 0, 시 * */
    private static final Pattern HOURLY = Pattern.compile("^0\\s+0\\s+\\*\\s+\\*\\s+\\*\\s+\\*$");
    /** 매일 지정 시각형: 초 0, 분 m, 시 H */
    private static final Pattern DAILY = Pattern.compile("^0\\s+(\\d{1,2})\\s+(\\d{1,2})\\s+\\*\\s+\\*\\s+\\*$");

    private final KeywordService keywordService;
    private final KeywordCollectionHistoryRepository collectionHistoryRepository;

    /** {@code KeywordNewsBatchScheduler} 와 동일한 프로퍼티·기본값을 쓴다. */
    @Value("${batch.schedule.economics-sync-cron:0 0 * * * *}")
    private String cronExpression;

    public CollectorScheduleResponse getSchedule(Long userId) {
        List<Long> keywordIds = keywordService.getSubscribedKeywordIds(userId);
        long failedKeywordCount = collectionHistoryRepository.summarizeByKeywordIds(keywordIds).stream()
                .filter(KeywordCollectionSummary::isFailing)
                .count();

        return new CollectorScheduleResponse(
                describeCron(cronExpression),
                cronExpression,
                collectionHistoryRepository.findLastAttemptAt().orElse(null),
                nextRunAt(),
                failedKeywordCount
        );
    }

    private LocalDateTime nextRunAt() {
        try {
            CronExpression parsed = CronExpression.parse(cronExpression);
            return parsed.next(LocalDateTime.now());
        } catch (IllegalArgumentException e) {
            // 설정이 잘못돼도 화면 전체를 실패시키지 않는다 — 다음 실행만 비운다
            log.warn("수집 cron 파싱 실패: cron={}, error={}", cronExpression, e.getMessage());
            return null;
        }
    }

    /**
     * cron 을 한국어 설명으로 바꾼다. 전체 cron 문법을 번역하지는 않고,
     * 실제로 쓰는 두 형태만 다루며 그 외에는 cron 문자열을 그대로 돌려준다 —
     * 억지로 해석하면 틀린 주기를 보여주게 된다.
     */
    private String describeCron(String cron) {
        if (cron == null || cron.isBlank()) {
            return "";
        }
        String trimmed = cron.trim();
        if (HOURLY.matcher(trimmed).matches()) {
            return "매시 정각 수집";
        }
        Matcher daily = DAILY.matcher(trimmed);
        if (daily.matches()) {
            return String.format("매일 %02d:%02d 수집",
                    Integer.parseInt(daily.group(2)), Integer.parseInt(daily.group(1)));
        }
        return trimmed;
    }
}
