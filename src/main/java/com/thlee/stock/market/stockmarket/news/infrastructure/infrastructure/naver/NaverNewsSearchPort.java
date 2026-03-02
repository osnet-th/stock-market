package com.thlee.stock.market.stockmarket.news.infrastructure.infrastructure.naver;

import com.thlee.stock.market.stockmarket.news.domain.model.NewsSearchResult;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.news.domain.service.NewsSearchPort;
import com.thlee.stock.market.stockmarket.news.infrastructure.infrastructure.common.HtmlTextCleaner;
import com.thlee.stock.market.stockmarket.news.infrastructure.infrastructure.naver.dto.NaverNewsItem;
import com.thlee.stock.market.stockmarket.news.infrastructure.infrastructure.naver.dto.NaverNewsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NaverNewsSearchPort implements NewsSearchPort {
    private final Region region = Region.DOMESTIC;
    private final NaverNewsApiClient apiClient;

    @Override
    public List<NewsSearchResult> search(String keyword, LocalDateTime fromDateTime) {
        NaverNewsResponse response = apiClient.search(keyword);
        return response.getItems().stream()
            .filter(item -> isAfterFromDateTime(item, fromDateTime))
            .map(this::toNewsSearchResult)
            .collect(Collectors.toList());
    }

    @Override
    public Region supportedRegion() {
        return region;
    }

    private boolean isAfterFromDateTime(NaverNewsItem item, LocalDateTime fromDateTime) {
        LocalDateTime publishedAt = parseNaverDateTime(item.getPubDate());
        return publishedAt.isAfter(fromDateTime) || publishedAt.isEqual(fromDateTime);
    }

    private LocalDateTime parseNaverDateTime(String pubDate) {
        DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(pubDate, formatter);
        return zonedDateTime.toLocalDateTime();
    }

    private NewsSearchResult toNewsSearchResult(NaverNewsItem item) {
        return new NewsSearchResult(
            HtmlTextCleaner.clean(item.getTitle()),
            item.getOriginallink() != null ? item.getOriginallink() : item.getLink(),
            HtmlTextCleaner.clean(item.getDescription()),
            parseNaverDateTime(item.getPubDate())
        );
    }
}