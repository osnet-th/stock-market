package com.thlee.stock.market.stockmarket.news.infrastructure.elasticsearch.document;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.time.LocalDate;

/**
 * 뉴스 Elasticsearch 문서 모델
 */
@Document(indexName = "news")
@Setting(settingPath = "elasticsearch/news-settings.json")
@Getter
public class NewsDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "korean_analyzer")
    private String title;

    @Field(type = FieldType.Text, analyzer = "korean_analyzer")
    private String content;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate publishedAt;

    @Field(type = FieldType.Keyword)
    private String region;

    @Field(type = FieldType.Long)
    private Long keywordId;

    @Field(type = FieldType.Keyword, index = false)
    private String originalUrl;

    protected NewsDocument() {
    }

    public NewsDocument(String id,
                        String title,
                        String content,
                        LocalDate publishedAt,
                        String region,
                        Long keywordId,
                        String originalUrl) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.publishedAt = publishedAt;
        this.region = region;
        this.keywordId = keywordId;
        this.originalUrl = originalUrl;
    }
}