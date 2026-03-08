package com.thlee.stock.market.stockmarket.portfolio.application;

import com.thlee.stock.market.stockmarket.news.application.dto.NewsResultDto;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;

record PortfolioNewsContext(
        Long userId,
        Long itemId,
        Region region,
        NewsResultDto news
) {
}