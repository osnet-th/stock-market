package com.thlee.stock.market.stockmarket.glossary.domain.repository;

/**
 * 용어 리스트 필터 value. 모든 검색 필드는 nullable (null/blank = 해당 필터 비활성).
 *
 * <ul>
 *   <li>{@code q} — 용어명 LIKE 검색어 (이미 LIKE 와일드카드 이스케이프된 상태)</li>
 *   <li>{@code definitionQ} — 설명 LIKE 검색어 (이미 LIKE 와일드카드 이스케이프된 상태)</li>
 *   <li>{@code categoryId} — null = 카테고리 필터 비활성, 특정 ID 또는 sentinel({@code Long.valueOf(-1L)} 등) 처리는 service 책임</li>
 *   <li>{@code uncategorizedOnly} — true = {@code category_id IS NULL} 필터</li>
 *   <li>{@code sort} — null 불가. default 는 {@link GlossaryTermSort#REGISTERED_DESC}</li>
 *   <li>{@code page} — 0-base, {@code size} 1~200</li>
 * </ul>
 */
public record GlossaryTermListFilter(
        String q,
        String definitionQ,
        Long categoryId,
        boolean uncategorizedOnly,
        GlossaryTermSort sort,
        int page,
        int size
) {
    public GlossaryTermListFilter {
        if (sort == null) {
            throw new IllegalArgumentException("sort 는 필수입니다.");
        }
        if (page < 0) {
            throw new IllegalArgumentException("page 는 0 이상이어야 합니다.");
        }
        if (size <= 0 || size > 200) {
            throw new IllegalArgumentException("size 는 1~200 범위여야 합니다.");
        }
    }
}