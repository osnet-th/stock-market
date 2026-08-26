package com.thlee.stock.market.stockmarket.glossary.presentation.dto;

import com.thlee.stock.market.stockmarket.glossary.application.dto.CreateGlossaryCategoryCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGlossaryCategoryRequest(
        @NotBlank @Size(max = 50) String name
) {
    public CreateGlossaryCategoryCommand toCommand() {
        return new CreateGlossaryCategoryCommand(name);
    }
}