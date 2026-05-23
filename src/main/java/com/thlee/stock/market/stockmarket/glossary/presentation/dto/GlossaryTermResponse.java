package com.thlee.stock.market.stockmarket.glossary.presentation.dto;

import com.thlee.stock.market.stockmarket.glossary.domain.model.GlossaryTerm;

import java.time.LocalDateTime;

public record GlossaryTermResponse(
        Long id,
        String name,
        String definition,
        Long categoryId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static GlossaryTermResponse from(GlossaryTerm t) {
        return new GlossaryTermResponse(
                t.getId(), t.getName(), t.getDefinition(), t.getCategoryId(),
                t.getCreatedAt(), t.getUpdatedAt()
        );
    }
}