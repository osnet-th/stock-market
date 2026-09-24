package com.thlee.stock.market.stockmarket.news.application.dto;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 수집 스케줄 상태 응답 (#115 통합 화면 헤더).
 *
 * <p>목업 헤더의 `● 매시 정각 수집 · 마지막 12분 전 · 다음 45분 후` 와
 * `연속 실패 N개 · 재시도` 뱃지를 그리는 데 쓴다.
 */
@Getter
public class CollectorScheduleResponse {

    /** 사람이 읽을 주기 설명. cron 을 해석할 수 없으면 cron 문자열 그대로. */
    private final String scheduleLabel;
    private final String cronExpression;

    /**
     * 마지막 수집 <b>시도</b> 시각. 수집 이력이 하나도 없으면 null
     * (이 기능 도입 전이거나 배치가 아직 안 돈 경우).
     */
    private final LocalDateTime lastRunAt;

    /** cron 으로 계산한 다음 실행 예정 시각. */
    private final LocalDateTime nextRunAt;

    /** 내 키워드 중 마지막 성공 이후 실패가 쌓인 키워드 수. 0 이면 정상. */
    private final long failedKeywordCount;

    public CollectorScheduleResponse(String scheduleLabel,
                                     String cronExpression,
                                     LocalDateTime lastRunAt,
                                     LocalDateTime nextRunAt,
                                     long failedKeywordCount) {
        this.scheduleLabel = scheduleLabel;
        this.cronExpression = cronExpression;
        this.lastRunAt = lastRunAt;
        this.nextRunAt = nextRunAt;
        this.failedKeywordCount = failedKeywordCount;
    }

    /** 실패 키워드가 하나라도 있으면 헤더 표시등을 경고색으로 바꾼다. */
    public boolean isHealthy() {
        return failedKeywordCount == 0;
    }
}
