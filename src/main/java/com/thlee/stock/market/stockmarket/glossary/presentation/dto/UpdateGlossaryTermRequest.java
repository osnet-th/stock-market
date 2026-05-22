package com.thlee.stock.market.stockmarket.glossary.presentation.dto;

import com.thlee.stock.market.stockmarket.glossary.application.dto.UpdateGlossaryTermCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 용어 전체 교체 Request (PUT). 카테고리 정규화 규칙은 {@link CreateGlossaryTermRequest} 와 동일.
 */
public record UpdateGlossaryTermRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 4000) String definition,
        Long categoryId,
        @Size(max = 50) String categoryName
) {
    public UpdateGlossaryTermCommand toCommand() {
        return new UpdateGlossaryTermCommand(name, definition, categoryId, categoryName);
    }
}