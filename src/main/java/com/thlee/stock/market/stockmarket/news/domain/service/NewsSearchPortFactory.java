package com.thlee.stock.market.stockmarket.news.domain.service;

import com.thlee.stock.market.stockmarket.news.domain.model.Region;

import java.util.List;

/**
 * Region별 NewsSearchPort 제공 팩토리
 */
public interface NewsSearchPortFactory {
    List<NewsSearchPort> getPorts(Region region);
}