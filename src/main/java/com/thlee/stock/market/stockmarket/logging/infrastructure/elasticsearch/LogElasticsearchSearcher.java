package com.thlee.stock.market.stockmarket.logging.infrastructure.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.cat.IndicesResponse;
import co.elastic.clients.elasticsearch.cat.indices.IndicesRecord;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.thlee.stock.market.stockmarket.logging.domain.model.LogDomain;
import com.thlee.stock.market.stockmarket.logging.infrastructure.elasticsearch.document.ApplicationLogDocument;
import com.thlee.stock.market.stockmarket.logging.presentation.dto.LogDailyCountResponse;
import com.thlee.stock.market.stockmarket.logging.presentation.dto.LogDiskUsageResponse;
import com.thlee.stock.market.stockmarket.logging.presentation.dto.LogSearchRequest;
import com.thlee.stock.market.stockmarket.logging.presentation.dto.LogSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 로그 인덱스 검색/집계/사용량 어댑터 (운영자 페이지 전용).
 *
 * <ul>
 *   <li>검색: {@code search_after} 페이징, {@code timestamp desc + _id asc} tie-break</li>
 *   <li>집계: {@code date_histogram} (window 에 따라 day/week 자동 선택)</li>
 *   <li>디스크: {@code _cat/indices} 로 {@code app-*} 합산</li>
 * </ul>
 *
 * ES 장애는 상위(Service) 에서 처리 — Searcher 는 원본 예외 전파.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogElasticsearchSearcher {

    private static final String INDEX_PREFIX = "app-";
    private static final long ES_TIMEOUT_SEC = 5L;
    private static final long DAY_HISTOGRAM_THRESHOLD_DAYS = 31L;

    private final ElasticsearchClient elasticsearchClient;
    private final ElasticsearchOperations elasticsearchOperations;

    public LogSearchResponse search(LogSearchRequest req) throws Exception {
        String indexPattern = indexPattern(req.domain());
        Query query = buildQuery(req);
        int size = req.size();

        // News 모듈과 동일 패턴: ElasticsearchOperations 사용 — Spring Data ES 가 저장 시 삽입하는
        // "_class" type hint 를 올바르게 처리. raw ElasticsearchClient 로 역직렬화하면 "Failed to decode response" 실패.
        NativeQueryBuilder qb = NativeQuery.builder()
                .withQuery(query)
                .withMaxResults(size)
                .withSort(SortOptions.of(so -> so.field(f -> f.field("timestamp").order(SortOrder.Desc))))
                .withSort(SortOptions.of(so -> so.field(f -> f.field("_doc").order(SortOrder.Asc))))
                .withTrackTotalHits(true);
        if (req.searchAfter() != null && !req.searchAfter().isEmpty()) {
            // 프론트는 쿼리스트링에 값을 모두 String 으로 직렬화해 보내지만 ES sort value 는
            // timestamp(long) + _doc(integer) 타입이라 숫자로 복원해야 정확히 매칭됨.
            qb.withSearchAfter(req.searchAfter().stream()
                    .map(LogElasticsearchSearcher::coerceSearchAfterValue)
                    .toList());
        }

        SearchHits<ApplicationLogDocument> searchHits = elasticsearchOperations.search(
                qb.build(),
                ApplicationLogDocument.class,
                IndexCoordinates.of(indexPattern)
        );

        List<LogSearchResponse.Item> items = new ArrayList<>();
        List<Object> nextSearchAfter = null;
        for (SearchHit<ApplicationLogDocument> hit : searchHits.getSearchHits()) {
            ApplicationLogDocument doc = hit.getContent();
            if (doc == null) {
                continue;
            }
            items.add(toItem(hit.getId(), doc));
            nextSearchAfter = hit.getSortValues();
        }
        long total = searchHits.getTotalHits();
        if (items.size() < size) {
            nextSearchAfter = null;
        }
        return new LogSearchResponse(items, total, nextSearchAfter);
    }

    public LogDailyCountResponse aggregateByDate(LogDomain domain, Instant from, Instant to) throws Exception {
        String indexPattern = indexPattern(domain);
        long days = Duration.between(from, to).toDays();
        CalendarInterval interval = days <= DAY_HISTOGRAM_THRESHOLD_DAYS ? CalendarInterval.Day : CalendarInterval.Week;
        String intervalLabel = interval == CalendarInterval.Day ? "day" : "week";

        Aggregation agg = Aggregation.of(a -> a.dateHistogram(dh -> dh
                .field("timestamp")
                .calendarInterval(interval)
                .minDocCount(0)
                .extendedBounds(eb -> eb
                        .min(co.elastic.clients.elasticsearch._types.aggregations.FieldDateMath.of(b -> b.expr(from.toString())))
                        .max(co.elastic.clients.elasticsearch._types.aggregations.FieldDateMath.of(b -> b.expr(to.toString())))
                )
        ));

        Query rangeOnly = Query.of(q -> q.range(r -> r.date(d -> d
                .field("timestamp")
                .gte(from.toString())
                .lte(to.toString())
        )));

        SearchResponse<Void> resp = elasticsearchClient.search(s -> s
                .index(indexPattern)
                .query(rangeOnly)
                .size(0)
                .timeout(ES_TIMEOUT_SEC + "s")
                .allowNoIndices(true)
                .ignoreUnavailable(true)
                .aggregations("byDate", agg)
        , Void.class);

        var aggregate = resp.aggregations().get("byDate");
        if (aggregate == null || !aggregate.isDateHistogram()) {
            return new LogDailyCountResponse(intervalLabel, Collections.emptyList());
        }
        List<LogDailyCountResponse.DailyCount> counts = new ArrayList<>();
        for (DateHistogramBucket bucket : aggregate.dateHistogram().buckets().array()) {
            LocalDate date = Instant.ofEpochMilli(bucket.key()).atZone(ZoneOffset.UTC).toLocalDate();
            counts.add(new LogDailyCountResponse.DailyCount(date, bucket.docCount()));
        }
        return new LogDailyCountResponse(intervalLabel, counts);
    }

    public LogDiskUsageResponse diskUsage() throws Exception {
        IndicesResponse resp = elasticsearchClient.cat().indices(c -> c
                .index(List.of(
                        INDEX_PREFIX + LogDomain.AUDIT.getIndexSuffix() + "-*",
                        INDEX_PREFIX + LogDomain.ERROR.getIndexSuffix() + "-*",
                        INDEX_PREFIX + LogDomain.BUSINESS.getIndexSuffix() + "-*"
                ))
                .bytes(co.elastic.clients.elasticsearch._types.Bytes.Bytes)
        );

        long totalBytes = 0;
        long totalDocs = 0;
        List<LogDiskUsageResponse.IndexUsage> perIndex = new ArrayList<>();
        for (IndicesRecord rec : resp.indices()) {
            String name = rec.index();
            long bytes = parseLong(rec.storeSize());
            long docs = parseLong(rec.docsCount());
            perIndex.add(new LogDiskUsageResponse.IndexUsage(name, docs, bytes));
            totalBytes += bytes;
            totalDocs += docs;
        }
        return new LogDiskUsageResponse(totalBytes, totalDocs, perIndex);
    }

    // ──────────────────────────────────────────────────────────────────
    // 내부 헬퍼
    // ──────────────────────────────────────────────────────────────────

    private String indexPattern(LogDomain domain) {
        return INDEX_PREFIX + domain.getIndexSuffix() + "-*";
    }

    private Query buildQuery(LogSearchRequest req) {
        BoolQuery.Builder bool = new BoolQuery.Builder();

        // timestamp range
        bool.filter(Query.of(q -> q.range(r -> r.date(d -> d
                .field("timestamp")
                .gte(req.from().toString())
                .lte(req.to().toString())
        ))));

        if (req.userId() != null) {
            bool.filter(Query.of(q -> q.term(TermQuery.of(t -> t
                    .field("userId")
                    .value(req.userId())
            ))));
        }
        if (req.status() != null) {
            bool.filter(Query.of(q -> q.term(TermQuery.of(t -> t
                    .field("status")
                    .value(req.status())
            ))));
        }
        if (req.exceptionClass() != null && !req.exceptionClass().isBlank()) {
            bool.filter(Query.of(q -> q.term(TermQuery.of(t -> t
                    .field("exceptionClass")
                    .value(req.exceptionClass())
            ))));
        }
        if (req.q() != null && !req.q().isBlank()) {
            // requestId 키워드 정확 매칭 (payload 내부는 enabled=false 라 검색 불가)
            bool.filter(Query.of(q -> q.term(TermQuery.of(t -> t
                    .field("requestId")
                    .value(req.q())
            ))));
        }
        return Query.of(q -> q.bool(bool.build()));
    }

    private LogSearchResponse.Item toItem(String id, ApplicationLogDocument doc) {
        return new LogSearchResponse.Item(
                id,
                doc.getTimestamp(),
                doc.getDomain(),
                doc.getUserId(),
                doc.getRequestId(),
                doc.getStatus(),
                doc.getExceptionClass(),
                doc.getPayload(),
                doc.isTruncated(),
                doc.getOriginalSize()
        );
    }

    /**
     * 쿼리스트링으로 String 형태로 전달된 searchAfter 값을 원본 타입으로 복원.
     * 숫자로 완전 파싱되면 Long/Double, 아니면 원본 String 유지.
     */
    private static Object coerceSearchAfterValue(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number) {
            return raw;
        }
        String s = raw.toString();
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException notLong) {
            // fallthrough
        }
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException notDouble) {
            return s;
        }
    }

    private static long parseLong(String value) {
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}