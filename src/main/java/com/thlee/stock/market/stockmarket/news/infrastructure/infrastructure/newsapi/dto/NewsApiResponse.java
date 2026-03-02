package com.thlee.stock.market.stockmarket.news.infrastructure.infrastructure.newsapi.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NewsApiResponse {
    private String status;
    private int totalResults;
    private List<NewsApiArticle> articles;
}