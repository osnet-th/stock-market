package com.thlee.stock.market.stockmarket.news.presentation.dto;

import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 뉴스 전문 검색 요청 DTO
 */
@Getter
@Setter
public class NewsSearchRequest {

    @NotBlank(message = "검색어는 필수입니다.")
    @Size(max = 200, message = "검색어는 200자 이내여야 합니다.")
    private String query;

    private LocalDate startDate;
    private LocalDate endDate;
    private Region region;

    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
    private int page = 0;

    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
    private int size = 20;
}