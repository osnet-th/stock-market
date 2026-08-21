package com.thlee.stock.market.stockmarket.glossary.application.dto;

import com.thlee.stock.market.stockmarket.glossary.domain.model.GlossaryTermContent;

import java.util.List;

/**
 * 용어 전체 교체 (PUT) 명령. 카테고리 결정 우선순위와 relatedTermIds 정규화 규칙은
 * {@link CreateGlossaryTermCommand} 와 동일.
 */
public record UpdateGlossaryTermCommand(
        String name,
        String abbreviation,
        String oneLine,
        String definition,
        String scaleNote,
        String example,
        String takeaway,
        Long categoryId,
        String categoryName,
        List<Long> relatedTermIds
) {
    /** 구조화 콘텐츠 묶음 — 도메인 VO 로 변환 (길이 검증은 VO 책임). */
    public GlossaryTermContent content() {
        return new GlossaryTermContent(abbreviation, oneLine, definition, scaleNote, example, takeaway);
    }
}
