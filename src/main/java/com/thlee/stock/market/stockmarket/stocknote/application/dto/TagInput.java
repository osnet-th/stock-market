package com.thlee.stock.market.stockmarket.stocknote.application.dto;

/**
 * 태그 입력 단위.
 *
 * <p>고정 태그: {@code source ∈ {TRIGGER, CHARACTER, SUPPLY}}, {@code value} 는 해당 enum 이름.
 * <p>자유 태그: {@code source = CUSTOM}, {@code value} 는 사용자 입력 (정규화 전).
 */
public record TagInput(String source, String value) {
    public TagInput {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("tag source 는 필수입니다.");
        }
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("tag value 는 필수입니다.");
        }
    }
}