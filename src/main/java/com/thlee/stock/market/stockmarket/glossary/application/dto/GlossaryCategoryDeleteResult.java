package com.thlee.stock.market.stockmarket.glossary.application.dto;

/**
 * 카테고리 삭제 결과. R6 cascade 응답에 포함된다.
 *
 * @param deletedCategoryId 삭제된 카테고리 ID
 * @param reassignedCount   "미분류"로 이동된 용어 개수
 */
public record GlossaryCategoryDeleteResult(
        Long deletedCategoryId,
        int reassignedCount
) {
}