package com.thlee.stock.market.stockmarket.news.infrastructure.elasticsearch.mapper;

import com.thlee.stock.market.stockmarket.news.domain.model.News;
import com.thlee.stock.market.stockmarket.news.infrastructure.elasticsearch.document.NewsDocument;

/**
 * News Domain Model → ES NewsDocument 변환 Mapper
 */
public class NewsDocumentMapper {

    public static NewsDocument toDocument(News news) {
        return new NewsDocument(
                news.getOriginalUrl(),
                news.getTitle(),
                news.getContent(),
                news.getPublishedAt() != null ? news.getPublishedAt().toLocalDate() : null,
                news.getRegion() != null ? news.getRegion().name() : null,
                news.getKeywordId(),
                news.getOriginalUrl()
        );
    }
}