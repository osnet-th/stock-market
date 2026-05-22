package com.thlee.stock.market.stockmarket.glossary.application.dto;

/**
 * 용어 생성 명령.
 *
 * <p>카테고리 결정 우선순위:
 * <ol>
 *   <li>{@code categoryId} 가 non-null → ownership 검증 후 그 ID 사용</li>
 *   <li>{@code categoryId} null + {@code categoryName} non-blank → R13 인라인 find-or-create</li>
 *   <li>둘 다 null/blank → null (미분류)</li>
 * </ol>
 */
public record CreateGlossaryTermCommand(
        String name,
        String definition,
        Long categoryId,
        String categoryName
) {
}