package com.thlee.stock.market.stockmarket.news.infrastructure.elasticsearch;

import com.thlee.stock.market.stockmarket.common.response.PageResult;
import com.thlee.stock.market.stockmarket.news.domain.model.News;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.news.domain.service.NewsFullTextSearchPort;
import com.thlee.stock.market.stockmarket.news.infrastructure.elasticsearch.document.NewsDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.DateRangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;

/**
 * ES 전문 검색 어댑터 — NewsFullTextSearchPort 구현
 * ES 장애 시 빈 결과를 반환하고 로그를 남긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewsElasticsearchSearcher implements NewsFullTextSearchPort {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public PageResult<News> search(String query, LocalDate startDate, LocalDate endDate,
                                   Region region, int page, int size) {
        try {
            NativeQuery searchQuery = buildSearchQuery(query, startDate, endDate, region, page, size);
            SearchHits<NewsDocument> searchHits = elasticsearchOperations.search(searchQuery, NewsDocument.class);

            List<News> newsList = searchHits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .map(this::toNews)
                    .toList();

            return new PageResult<>(newsList, page, size, searchHits.getTotalHits());
        } catch (Exception e) {
            log.warn("ES 뉴스 검색 실패: query={}, error={}", query, e.getMessage(), e);
            return new PageResult<>(Collections.emptyList(), page, size, 0);
        }
    }

    private NativeQuery buildSearchQuery(String query, LocalDate startDate, LocalDate endDate,
                                         Region region, int page, int size) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // multi_match: title^2, content
        boolBuilder.must(Query.of(q -> q.multiMatch(MultiMatchQuery.of(mm -> mm
                .query(query)
                .fields("title^2", "content")
        ))));

        // date range filter
        if (startDate != null || endDate != null) {
            boolBuilder.filter(Query.of(q -> q.range(r -> r.date(DateRangeQuery.of(d -> {
                var builder = d.field("publishedAt");
                if (startDate != null) {
                    builder.gte(startDate.atStartOfDay().toString());
                }
                if (endDate != null) {
                    builder.lte(endDate.atTime(23, 59, 59).toString());
                }
                return builder;
            })))));
        }

        // region filter
        if (region != null) {
            boolBuilder.filter(Query.of(q -> q.term(TermQuery.of(t -> t
                    .field("region")
                    .value(region.name())
            ))));
        }

        return NativeQuery.builder()
                .withQuery(Query.of(q -> q.bool(boolBuilder.build())))
                .withPageable(PageRequest.of(page, size))
                .build();
    }

    private News toNews(NewsDocument doc) {
        Region region = null;
        if (doc.getRegion() != null) {
            try {
                region = Region.valueOf(doc.getRegion());
            } catch (IllegalArgumentException e) {
                log.warn("알 수 없는 Region 값: {}", doc.getRegion());
            }
        }

        return new News(
                null,
                doc.getOriginalUrl(),
                doc.getTitle(),
                doc.getContent(),
                doc.getPublishedAt() != null ? doc.getPublishedAt().atStartOfDay() : null,
                LocalDateTime.now(),
                doc.getKeywordId(),
                region
        );
    }
}