package com.thlee.stock.market.stockmarket.news.domain.model;

import java.time.LocalDateTime;

/**
 * 키워드 도메인 모델 (공유 리소스)
 */
public class Keyword {
    private Long id;
    private String keyword;
    private Region region;
    private LocalDateTime createdAt;

    private Keyword(String keyword, Region region, LocalDateTime createdAt) {
        this.keyword = keyword;
        this.region = region;
        this.createdAt = createdAt;
    }

    /**
     * 재구성용 생성자 (Repository에서 조회 시 사용)
     */
    public Keyword(Long id, String keyword, Region region, LocalDateTime createdAt) {
        this.id = id;
        this.keyword = keyword;
        this.region = region;
        this.createdAt = createdAt;
    }

    /**
     * 키워드 생성
     */
    public static Keyword create(String keyword, Region region) {
        validateKeyword(keyword);
        validateRegion(region);

        return new Keyword(keyword, region, LocalDateTime.now());
    }

    private static void validateKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("키워드는 필수입니다.");
        }
    }

    private static void validateRegion(Region region) {
        if (region == null) {
            throw new IllegalArgumentException("region은 필수입니다.");
        }
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getKeyword() {
        return keyword;
    }

    public Region getRegion() {
        return region;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
