package com.thlee.stock.market.stockmarket.news.infrastructure.elasticsearch;

import com.thlee.stock.market.stockmarket.common.response.PageResult;
import com.thlee.stock.market.stockmarket.news.domain.model.News;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsSearchCriteria;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsSearchField;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsSearchSort;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.news.domain.service.NewsFullTextSearchPort;
import com.thlee.stock.market.stockmarket.news.infrastructure.elasticsearch.document.NewsDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.DateRangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;

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
    public PageResult<News> search(NewsSearchCriteria criteria) {
        int page = criteria.page();
        int size = criteria.size();
        try {
            NativeQuery searchQuery = buildSearchQuery(criteria);
            SearchHits<NewsDocument> searchHits = elasticsearchOperations.search(searchQuery, NewsDocument.class);

            List<News> newsList = searchHits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .map(this::toNews)
                    .toList();

            return new PageResult<>(newsList, page, size, searchHits.getTotalHits());
        } catch (Exception e) {
            log.warn("ES 뉴스 검색 실패: query={}, error={}", criteria.query(), e.getMessage(), e);
            return new PageResult<>(Collections.emptyList(), page, size, 0);
        }
    }

    private NativeQuery buildSearchQuery(NewsSearchCriteria criteria) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // multi_match — 목업의 `제목만` 은 content 를 제외한다 (#115)
        String[] fields = criteria.effectiveField() == NewsSearchField.TITLE
                ? new String[]{"title"}
                : new String[]{"title^2", "content"};
        boolBuilder.must(Query.of(q -> q.multiMatch(MultiMatchQuery.of(mm -> mm
                .query(criteria.query())
                .fields(List.of(fields))
        ))));

        // 키워드 스코프 — 내 구독 키워드로 좁힌다. 컨트롤러가 소유권을 확정해 넘긴 값이다 (#115)
        if (!criteria.hasNoKeywordScope()) {
            List<FieldValue> keywordValues = criteria.keywordIds().stream()
                    .map(FieldValue::of)
                    .toList();
            boolBuilder.filter(Query.of(q -> q.terms(TermsQuery.of(t -> t
                    .field("keywordId")
                    .terms(TermsQueryField.of(f -> f.value(keywordValues)))
            ))));
        }

        // date range filter
        LocalDate startDate = criteria.startDate();
        LocalDate endDate = criteria.endDate();
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
        if (criteria.region() != null) {
            boolBuilder.filter(Query.of(q -> q.term(TermQuery.of(t -> t
                    .field("region")
                    .value(criteria.region().name())
            ))));
        }

        // 관련도는 ES 기본 정렬(_score)이라 지정하지 않는다. 최신순만 명시한다 (#115)
        var builder = NativeQuery.builder()
                .withQuery(Query.of(q -> q.bool(boolBuilder.build())))
                .withPageable(PageRequest.of(criteria.page(), criteria.size()));
        if (criteria.effectiveSort() == NewsSearchSort.LATEST) {
            builder.withSort(Sort.by(Sort.Direction.DESC, "publishedAt"));
        }
        return builder.build();
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

        // id · source 는 ES 문서에 없다 (문서 _id 는 originalUrl 이고 언론사는 색인하지 않는다).
        // 검색 결과는 originalUrl 로 DB 를 다시 조회해 보강하므로 여기서는 비워 둔다 (#115).
        return new News(
                null,
                doc.getOriginalUrl(),
                doc.getTitle(),
                doc.getContent(),
                doc.getPublishedAt() != null ? doc.getPublishedAt().atStartOfDay() : null,
                LocalDateTime.now(),
                doc.getKeywordId(),
                region,
                null
        );
    }
}