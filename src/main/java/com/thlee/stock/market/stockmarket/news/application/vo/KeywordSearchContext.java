package com.thlee.stock.market.stockmarket.news.application.vo;

import com.thlee.stock.market.stockmarket.news.application.dto.NewsResultDto;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;

public record KeywordSearchContext(
        Long userId,
        Long keywordId,
        Region region,
        NewsResultDto news
) {
}