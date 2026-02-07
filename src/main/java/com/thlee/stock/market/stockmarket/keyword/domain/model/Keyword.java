package com.thlee.stock.market.stockmarket.keyword.domain.model;

import java.time.LocalDateTime;

/**
 * 키워드 도메인 모델
 */
public class Keyword {
    private Long id;
    private String keyword;
    private Long userId;
    private boolean active;
    private LocalDateTime createdAt;

    private Keyword(String keyword, Long userId, boolean active, LocalDateTime createdAt) {
        this.keyword = keyword;
        this.userId = userId;
        this.active = active;
        this.createdAt = createdAt;
    }

    /**
     * 재구성용 생성자 (Repository에서 조회 시 사용)
     */
    public Keyword(Long id, String keyword, Long userId, boolean active, LocalDateTime createdAt) {
        this.id = id;
        this.keyword = keyword;
        this.userId = userId;
        this.active = active;
        this.createdAt = createdAt;
    }

    /**
     * 키워드 생성
     */
    public static Keyword create(String keyword, Long userId) {
        validateKeyword(keyword);
        validateUserId(userId);

        return new Keyword(keyword, userId, true, LocalDateTime.now());
    }

    private static void validateKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("키워드는 필수입니다.");
        }
    }

    private static void validateUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }
    }

    /**
     * 키워드 비활성화
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * 키워드 활성화
     */
    public void activate() {
        this.active = true;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getKeyword() {
        return keyword;
    }

    public Long getUserId() {
        return userId;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}