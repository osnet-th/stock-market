package com.thlee.stock.market.stockmarket.glossary.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.glossary.domain.model.GlossaryCategory;
import com.thlee.stock.market.stockmarket.glossary.domain.model.GlossaryTerm;
import com.thlee.stock.market.stockmarket.glossary.infrastructure.persistence.GlossaryCategoryEntity;
import com.thlee.stock.market.stockmarket.glossary.infrastructure.persistence.GlossaryTermEntity;

/**
 * glossary 도메인의 Entity ↔ Domain 변환 모음.
 * {@code categoryId == null} (미분류) 라운드트립 보존을 책임진다.
 */
public final class GlossaryMapper {

    private GlossaryMapper() {
    }

    // -------- GlossaryCategory --------
    public static GlossaryCategoryEntity toEntity(GlossaryCategory d) {
        return new GlossaryCategoryEntity(
                d.getId(), d.getUserId(), d.getName(), d.getCreatedAt(), d.getUpdatedAt()
        );
    }

    public static GlossaryCategory toDomain(GlossaryCategoryEntity e) {
        return new GlossaryCategory(
                e.getId(), e.getUserId(), e.getName(), e.getCreatedAt(), e.getUpdatedAt()
        );
    }

    // -------- GlossaryTerm --------
    public static GlossaryTermEntity toEntity(GlossaryTerm d) {
        return new GlossaryTermEntity(
                d.getId(), d.getUserId(), d.getName(), d.getDefinition(), d.getCategoryId(),
                d.getCreatedAt(), d.getUpdatedAt()
        );
    }

    public static GlossaryTerm toDomain(GlossaryTermEntity e) {
        return new GlossaryTerm(
                e.getId(), e.getUserId(), e.getName(), e.getDefinition(), e.getCategoryId(),
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}