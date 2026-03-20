package com.thlee.stock.market.stockmarket.news.domain.model;

import java.time.LocalDateTime;

/**
 * 사용자-키워드 구독 관계 도메인 모델
 */
public class UserKeyword {

    private Long id;
    private Long userId;
    private Long keywordId;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private UserKeyword(Long userId, Long keywordId, boolean active,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.userId = userId;
        this.keywordId = keywordId;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 재구성용 생성자 (Repository에서 조회 시 사용)
     */
    public UserKeyword(Long id, Long userId, Long keywordId, boolean active,
                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.keywordId = keywordId;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 구독 생성
     */
    public static UserKeyword create(Long userId, Long keywordId) {
        validateUserId(userId);
        validateKeywordId(keywordId);
        LocalDateTime now = LocalDateTime.now();
        return new UserKeyword(userId, keywordId, true, now, now);
    }

    public void activate() {
        this.active = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }

    private static void validateUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }
    }

    private static void validateKeywordId(Long keywordId) {
        if (keywordId == null) {
            throw new IllegalArgumentException("keywordId는 필수입니다.");
        }
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getKeywordId() {
        return keywordId;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}