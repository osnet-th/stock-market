package com.thlee.stock.market.stockmarket.economics.derivedindicator.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 파생지표 수정 요청. 생성과 동일한 검증 적용.
 */
public record DerivedIndicatorUpdateRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 30) String unit,
        @Size(max = 500) String description,
        @NotNull @Valid DerivedFormulaRequest formula
) {
}
