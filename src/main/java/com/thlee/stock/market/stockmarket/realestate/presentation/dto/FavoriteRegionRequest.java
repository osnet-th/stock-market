package com.thlee.stock.market.stockmarket.realestate.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 관심지역 등록 요청. emdCode가 null이면 시군구 단위 즐겨찾기.
 */
public record FavoriteRegionRequest(
        @NotBlank
        @Pattern(regexp = "^(\\d{2}|\\d{5})$",
                message = "regionCode는 2자리(시도) 또는 5자리(시군구) 숫자여야 합니다")
        String regionCode,

        @Pattern(regexp = "\\d{8}", message = "emdCode는 8자리 숫자여야 합니다")
        String emdCode
) {
}