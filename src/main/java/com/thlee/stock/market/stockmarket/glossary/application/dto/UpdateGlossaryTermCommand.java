package com.thlee.stock.market.stockmarket.glossary.application.dto;

/**
 * 용어 전체 교체 (PUT) 명령. 카테고리 결정 우선순위는
 * {@link CreateGlossaryTermCommand} 와 동일.
 */
public record UpdateGlossaryTermCommand(
        String name,
        String definition,
        Long categoryId,
        String categoryName
) {
}