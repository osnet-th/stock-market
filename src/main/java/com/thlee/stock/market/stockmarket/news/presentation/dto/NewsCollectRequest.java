package com.thlee.stock.market.stockmarket.news.presentation.dto;

import com.thlee.stock.market.stockmarket.news.domain.model.NewsPurpose;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NewsCollectRequest {
    private NewsPurpose purpose;
    private Long sourceId;
    private String keyword;
    private Long userId;
    private Region region;
}