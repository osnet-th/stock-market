package com.thlee.stock.market.stockmarket.newsjournal.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEvent;
import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEventCategory;
import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEventLink;
import com.thlee.stock.market.stockmarket.newsjournal.infrastructure.persistence.NewsEventCategoryEntity;
import com.thlee.stock.market.stockmarket.newsjournal.infrastructure.persistence.NewsEventEntity;
import com.thlee.stock.market.stockmarket.newsjournal.infrastructure.persistence.NewsEventLinkEntity;

/**
 * newsjournal 도메인의 Entity ↔ Domain 변환 모음.
 * 본체({@link NewsEvent}) + 자식({@link NewsEventLink}) + 카테고리({@link NewsEventCategory}) × 2 방향.
 */
public final class NewsEventMapper {

    private NewsEventMapper() {
    }

    // -------- NewsEvent --------
    public static NewsEventEntity toEntity(NewsEvent d) {
        return new NewsEventEntity(
                d.getId(), d.getUserId(), d.getTitle(), d.getOccurredDate(), d.getImpact(),
                d.getCategoryId(), d.getWhat(), d.getWhy(), d.getHow(),
                d.getCreatedAt(), d.getUpdatedAt()
        );
    }

    public static NewsEvent toDomain(NewsEventEntity e) {
        return new NewsEvent(
                e.getId(), e.getUserId(), e.getTitle(), e.getOccurredDate(), e.getImpact(),
                e.getCategoryId(), e.getWhat(), e.getWhy(), e.getHow(),
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }

    // -------- NewsEventLink --------
    public static NewsEventLinkEntity toEntity(NewsEventLink d) {
        return new NewsEventLinkEntity(
                d.getId(), d.getEventId(), d.getTitle(), d.getUrl(), d.getDisplayOrder()
        );
    }

    public static NewsEventLink toDomain(NewsEventLinkEntity e) {
        return new NewsEventLink(
                e.getId(), e.getEventId(), e.getTitle(), e.getUrl(), e.getDisplayOrder()
        );
    }

    // -------- NewsEventCategory --------
    public static NewsEventCategoryEntity toEntity(NewsEventCategory d) {
        return new NewsEventCategoryEntity(
                d.getId(), d.getUserId(), d.getName(), d.getCreatedAt()
        );
    }

    public static NewsEventCategory toDomain(NewsEventCategoryEntity e) {
        return new NewsEventCategory(
                e.getId(), e.getUserId(), e.getName(), e.getCreatedAt()
        );
    }
}