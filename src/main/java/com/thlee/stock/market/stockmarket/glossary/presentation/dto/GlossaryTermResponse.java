package com.thlee.stock.market.stockmarket.glossary.presentation.dto;

import com.thlee.stock.market.stockmarket.glossary.domain.model.GlossaryTerm;
import com.thlee.stock.market.stockmarket.glossary.domain.model.GlossaryTermContent;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 용어 단건 Response. {@code definition} 은 화면에서 '풀이' 로 노출된다
 * (필드명은 기존 API 하위호환 유지).
 */
public record GlossaryTermResponse(
        Long id,
        String name,
        String abbreviation,
        String oneLine,
        String definition,
        String scaleNote,
        String example,
        String takeaway,
        Long categoryId,
        List<Long> relatedTermIds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static GlossaryTermResponse from(GlossaryTerm t) {
        GlossaryTermContent c = t.getContent();
        return new GlossaryTermResponse(
                t.getId(), t.getName(),
                c.abbreviation(), c.oneLine(), c.definition(), c.scaleNote(), c.example(), c.takeaway(),
                t.getCategoryId(), t.getRelatedTermIds(),
                t.getCreatedAt(), t.getUpdatedAt()
        );
    }
}
