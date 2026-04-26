package com.thlee.stock.market.stockmarket.stocknote.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request/Response 공용 태그 페이로드.
 *
 * <p>{@code source} 는 고정 4값(TRIGGER/CHARACTER/SUPPLY/CUSTOM), {@code value} 는 32자 이하.
 */
public record TagPayload(
        @NotBlank
        @Pattern(regexp = "^(TRIGGER|CHARACTER|SUPPLY|CUSTOM)$",
                message = "tagSource 는 TRIGGER/CHARACTER/SUPPLY/CUSTOM 중 하나여야 합니다.")
        String source,

        @NotBlank
        @Size(max = 32)
        String value
) { }