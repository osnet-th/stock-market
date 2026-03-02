package com.thlee.stock.market.stockmarket.news.infrastructure.infrastructure.naver.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NaverNewsItem {
    private String title;
    private String originallink;
    private String link;
    private String description;
    private String pubDate;
}