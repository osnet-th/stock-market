package com.thlee.stock.market.stockmarket.mcp.presentation.dto;

import com.thlee.stock.market.stockmarket.common.response.PageResult;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsDto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 내 구독 키워드 범위의 저장 뉴스 (Claude 응답용). 본문은 앞부분만 담는다.
 */
public record McpNewsPage(List<Article> items, int page, int size, long totalElements, int totalPages) {

    static final int CONTENT_PREVIEW_LENGTH = 300;

    public record Article(Long id, String title, String summary, String source, String url,
                          LocalDateTime publishedAt) {}

    public static McpNewsPage from(PageResult<NewsDto> page) {
        return new McpNewsPage(page.getContent().stream()
                .map(n -> new Article(n.getId(), n.getTitle(), preview(n.getContent()), n.getSource(),
                        n.getOriginalUrl(), n.getPublishedAt()))
                .toList(), page.getPage(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private static String preview(String content) {
        if (content == null || content.length() <= CONTENT_PREVIEW_LENGTH) {
            return content;
        }
        return content.substring(0, CONTENT_PREVIEW_LENGTH) + "…";
    }
}
