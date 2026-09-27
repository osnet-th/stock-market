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
    /** NewsAPI 는 언론사를 중첩 객체로 준다 (#115). 기존 DTO 가 버리고 있어 살렸다. */
    private Source source;

    @Getter
    @Setter
    public static class Source {
        private String id;
        private String name;
    }
}