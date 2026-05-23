package com.thlee.stock.market.stockmarket.glossary.presentation.dto;

import com.thlee.stock.market.stockmarket.glossary.application.dto.CreateGlossaryTermCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 용어 생성 Request.
 *
 * <p>{@code categoryId} / {@code categoryName} 동시 입력 시 categoryId 우선 (service 정규화).
 */
public record CreateGlossaryTermRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 4000) String definition,
        Long categoryId,
        @Size(max = 50) String categoryName
) {
    public CreateGlossaryTermCommand toCommand() {
        return new CreateGlossaryTermCommand(name, definition, categoryId, categoryName);
    }
}