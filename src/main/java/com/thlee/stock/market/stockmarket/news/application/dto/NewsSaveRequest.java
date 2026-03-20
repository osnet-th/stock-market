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
    private final String title;
    private final String content;
    private final LocalDateTime publishedAt;
    private final Long keywordId;
    private final Region region;

    public NewsSaveRequest(String originalUrl,
                           String title,
                           String content,
                           LocalDateTime publishedAt,
                           Long keywordId,
                           Region region) {
        this.originalUrl = originalUrl;
        this.title = title;
        this.content = content;
        this.publishedAt = publishedAt;
        this.keywordId = keywordId;
        this.region = region;
    }
}
