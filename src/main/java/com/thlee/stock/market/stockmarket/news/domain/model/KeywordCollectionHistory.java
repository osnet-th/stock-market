package com.thlee.stock.market.stockmarket.news.domain.model;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 키워드 수집 시도 이력 (#115).
 *
 * <p>이 이력이 없으면 알 수 없는 것들이 있어 도입했다.
 * <ul>
 *   <li>마지막 <b>수집 성공</b> 시각 — 기존에는 {@code MAX(news.created_at)} 으로 근사했는데,
 *       그건 "마지막으로 기사가 저장된 시각"이라 새 기사가 없던 성공을 놓친다</li>
 *   <li>연속 실패 — 목업 헤더 뱃지와 레일 메타에 필요하다</li>
 *   <li>배치 마지막 실행 시각 — 스케줄 상태 표시</li>
 * </ul>
 */
@Getter
public class KeywordCollectionHistory {

    private Long id;
    private final Long keywordId;
    private final LocalDateTime attemptedAt;
    private final CollectionStatus status;
    private final int savedCount;
    private final int ignoredCount;
    private final String errorMessage;

    public KeywordCollectionHistory(Long id,
                                    Long keywordId,
                                    LocalDateTime attemptedAt,
                                    CollectionStatus status,
                                    int savedCount,
                                    int ignoredCount,
                                    String errorMessage) {
        this.id = id;
        this.keywordId = keywordId;
        this.attemptedAt = attemptedAt;
        this.status = status;
        this.savedCount = savedCount;
        this.ignoredCount = ignoredCount;
        this.errorMessage = errorMessage;
    }

    public static KeywordCollectionHistory success(Long keywordId, int savedCount, int ignoredCount) {
        return new KeywordCollectionHistory(
                null, keywordId, LocalDateTime.now(), CollectionStatus.SUCCESS,
                savedCount, ignoredCount, null
        );
    }

    /**
     * 실패 이력. 원인 메시지는 컬럼 길이에 맞춰 잘라 담는다 —
     * 스택트레이스가 길어 저장이 실패하면 이력 자체를 잃는다.
     */
    public static KeywordCollectionHistory failure(Long keywordId, String errorMessage) {
        return new KeywordCollectionHistory(
                null, keywordId, LocalDateTime.now(), CollectionStatus.FAILURE,
                0, 0, truncate(errorMessage)
        );
    }

    private static String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= MAX_ERROR_MESSAGE_LENGTH
                ? message
                : message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }

    public static final int MAX_ERROR_MESSAGE_LENGTH = 500;
}
