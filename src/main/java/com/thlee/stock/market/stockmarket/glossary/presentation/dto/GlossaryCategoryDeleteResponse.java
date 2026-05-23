package com.thlee.stock.market.stockmarket.glossary.presentation.dto;

import com.thlee.stock.market.stockmarket.glossary.application.dto.GlossaryCategoryDeleteResult;

/**
 * R6 cascade 응답: 삭제된 카테고리 ID + 미분류로 이동된 용어 수.
 */
public record GlossaryCategoryDeleteResponse(
        Long deletedCategoryId,
        int reassignedCount
) {
    public static GlossaryCategoryDeleteResponse from(GlossaryCategoryDeleteResult r) {
        return new GlossaryCategoryDeleteResponse(r.deletedCategoryId(), r.reassignedCount());
    }
}