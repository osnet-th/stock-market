package com.thlee.stock.market.stockmarket.glossary.presentation.dto;

import com.thlee.stock.market.stockmarket.glossary.domain.model.GlossaryCategory;

import java.time.LocalDateTime;

public record GlossaryCategoryResponse(
        Long id,
        String name,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static GlossaryCategoryResponse from(GlossaryCategory c) {
        return new GlossaryCategoryResponse(c.getId(), c.getName(), c.getCreatedAt(), c.getUpdatedAt());
    }
}