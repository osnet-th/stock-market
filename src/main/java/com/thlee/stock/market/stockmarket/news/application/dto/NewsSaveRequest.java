package com.thlee.stock.market.stockmarket.news.application.dto;

import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 뉴스 저장 요청 DTO
 */
@Getter
public class NewsSaveRequest {
    private final String originalUrl;
    private final Long userId;
    private final String title;
    private final String content;
    private final LocalDateTime publishedAt;
    private final Long sourceId;
    private final Region region;

    public NewsSaveRequest(String originalUrl,
                           Long userId,
                           String title,
                           String content,
                           LocalDateTime publishedAt,
                           Long sourceId,
                           Region region) {
        this.originalUrl = originalUrl;
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.publishedAt = publishedAt;
        this.sourceId = sourceId;
        this.region = region;
    }

}
