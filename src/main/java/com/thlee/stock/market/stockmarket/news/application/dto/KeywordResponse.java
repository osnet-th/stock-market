package com.thlee.stock.market.stockmarket.news.application.dto;

import com.thlee.stock.market.stockmarket.news.domain.model.Keyword;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.news.domain.model.UserKeyword;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 키워드 조회 응답 DTO (UserKeyword의 active 포함)
 */
@Getter
public class KeywordResponse {
    private final Long id;
    private final String keyword;
    private final Region region;
    private final boolean active;
    private final LocalDateTime createdAt;

    public KeywordResponse(Long id, String keyword, Region region, boolean active, LocalDateTime createdAt) {
        this.id = id;
        this.keyword = keyword;
        this.region = region;
        this.active = active;
        this.createdAt = createdAt;
    }

    public static KeywordResponse from(Keyword keyword, UserKeyword userKeyword) {
        return new KeywordResponse(
                keyword.getId(),
                keyword.getKeyword(),
                keyword.getRegion(),
                userKeyword.isActive(),
                keyword.getCreatedAt()
        );
    }
}