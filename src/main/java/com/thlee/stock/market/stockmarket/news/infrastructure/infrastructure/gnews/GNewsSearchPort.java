package com.thlee.stock.market.stockmarket.news.infrastructure.infrastructure.gnews;

import com.thlee.stock.market.stockmarket.news.domain.model.NewsSearchResult;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.news.domain.service.NewsSearchPort;
import com.thlee.stock.market.stockmarket.news.infrastructure.infrastructure.gnews.dto.GNewsArticle;
import com.thlee.stock.market.stockmarket.news.infrastructure.infrastructure.gnews.dto.GNewsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GNewsSearchPort implements NewsSearchPort {
    private final GNewsApiClient apiClient;
    private final Region region = Region.INTERNATIONAL;

    @Override
    public List<NewsSearchResult> search(String keyword, LocalDateTime fromDateTime) {
        String fromIso = fromDateTime.atZone(ZoneId.of("UTC"))
            .format(DateTimeFormatter.ISO_INSTANT);

        GNewsResponse response = apiClient.search(keyword, fromIso);
        return response.getArticles().stream()
            .map(this::toNewsSearchResult)
            .collect(Collectors.toList());
    }

    @Override
    public Region supportedRegion() {
        return region;
    }

    private NewsSearchResult toNewsSearchResult(GNewsArticle article) {
        return new NewsSearchResult(
            article.getTitle(),
            article.getUrl(),
            article.getDescription() != null ? article.getDescription() : article.getContent(),
            parseIsoDateTime(article.getPublishedAt())
        );
    }

    private LocalDateTime parseIsoDateTime(String publishedAt) {
        return ZonedDateTime.parse(publishedAt, DateTimeFormatter.ISO_DATE_TIME)
            .toLocalDateTime();
    }
}