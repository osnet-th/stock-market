package com.thlee.stock.market.stockmarket.news.infrastructure.infrastructure.gnews.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GNewsArticle {
    private String id;
    private String title;
    private String description;
    private String content;
    private String url;
    private String publishedAt;
}