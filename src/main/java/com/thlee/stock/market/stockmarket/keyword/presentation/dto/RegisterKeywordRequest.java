package com.thlee.stock.market.stockmarket.keyword.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 키워드 등록 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterKeywordRequest {
    private String keyword;
    private Long userId;
}
