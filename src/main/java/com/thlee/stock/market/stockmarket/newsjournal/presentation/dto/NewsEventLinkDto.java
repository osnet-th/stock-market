package com.thlee.stock.market.stockmarket.newsjournal.presentation.dto;

import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEventLink;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 사건 링크 입출력 공용 DTO. 입력 시 displayOrder 는 배열 인덱스로 부여되므로 본 DTO 에는 없음.
 */
public record NewsEventLinkDto(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 2000) String url
) {
    public static NewsEventLinkDto from(NewsEventLink l) {
        return new NewsEventLinkDto(l.getTitle(), l.getUrl());
    }
}