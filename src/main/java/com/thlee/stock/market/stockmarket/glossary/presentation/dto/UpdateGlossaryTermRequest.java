package com.thlee.stock.market.stockmarket.glossary.presentation.dto;

import com.thlee.stock.market.stockmarket.glossary.application.dto.UpdateGlossaryTermCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 용어 전체 교체 Request (PUT). 카테고리 정규화 규칙은 {@link CreateGlossaryTermRequest} 와 동일.
 * PUT semantics — 생략(null) 필드는 비움으로 교체된다.
 */
public record UpdateGlossaryTermRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 200) String abbreviation,
        @Size(max = 300) String oneLine,
        @Size(max = 4000) String definition,
        @Size(max = 4000) String scaleNote,
        @Size(max = 4000) String example,
        @Size(max = 4000) String takeaway,
        Long categoryId,
        @Size(max = 50) String categoryName,
        @Size(max = 20) List<Long> relatedTermIds
) {
    public UpdateGlossaryTermCommand toCommand() {
        return new UpdateGlossaryTermCommand(
                name, abbreviation, oneLine, definition, scaleNote, example, takeaway,
                categoryId, categoryName, relatedTermIds
        );
    }
}
