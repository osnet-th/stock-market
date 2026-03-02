package com.thlee.stock.market.stockmarket.news.infrastructure.infrastructure.newsapi.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NewsApiArticle {
    private String title;
    private String description;
    private String url;
    private String publishedAt;
    private String content;
}