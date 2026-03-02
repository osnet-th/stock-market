package com.thlee.stock.market.stockmarket.news.infrastructure.infrastructure.gnews.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GNewsResponse {
    private List<GNewsArticle> articles;
}