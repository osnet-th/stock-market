package com.thlee.stock.market.stockmarket.economics.derivedindicator.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 파생지표 생성 요청. name/unit/description은 사용자 자유 입력 — 서버 길이 검증 필수.
 * 출력 시 프론트는 x-text로만 렌더(저장형 XSS 방지).
 */
public record DerivedIndicatorCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 30) String unit,
        @Size(max = 500) String description,
        @NotNull @Valid DerivedFormulaRequest formula
) {
}
