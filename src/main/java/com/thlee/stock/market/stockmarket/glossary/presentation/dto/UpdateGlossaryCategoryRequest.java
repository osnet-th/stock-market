package com.thlee.stock.market.stockmarket.glossary.presentation.dto;

import com.thlee.stock.market.stockmarket.glossary.application.dto.UpdateGlossaryCategoryCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateGlossaryCategoryRequest(
        @NotBlank @Size(max = 50) String name
) {
    public UpdateGlossaryCategoryCommand toCommand() {
        return new UpdateGlossaryCategoryCommand(name);
    }
}