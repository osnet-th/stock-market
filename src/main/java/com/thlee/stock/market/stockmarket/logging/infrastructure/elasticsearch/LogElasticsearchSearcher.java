package com.thlee.stock.market.stockmarket.logging.infrastructure.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
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

    public LogSearchResponse search(LogSearchRequest req) throws Exception {
        String indexPattern = indexPattern(req.domain());
        Query query = buildQuery(req);
        int size = req.size();

        var searchReq = co.elastic.clients.elasticsearch.core.SearchRequest.of(s -> {
            var b = s.index(indexPattern)
                    .query(query)
                    .size(size)
                    .timeout(ES_TIMEOUT_SEC + "s")
                    .sort(so -> so.field(f -> f.field("timestamp").order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)))
                    .sort(so -> so.field(f -> f.field("_id").order(co.elastic.clients.elasticsearch._types.SortOrder.Asc)))
                    .trackTotalHits(t -> t.enabled(true))
                    .allowNoIndices(true)
                    .ignoreUnavailable(true);
            if (req.searchAfter() != null && !req.searchAfter().isEmpty()) {
                b.searchAfter(req.searchAfter().stream()
                        .map(LogElasticsearchSearcher::toFieldValue)
                        .toList());
            }
            return b;
        });

        SearchResponse<ApplicationLogDocument> resp = elasticsearchClient.search(searchReq, ApplicationLogDocument.class);

        List<LogSearchResponse.Item> items = new ArrayList<>();
        List<Object> nextSearchAfter = null;
        var hits = resp.hits().hits();
        for (var hit : hits) {
            ApplicationLogDocument doc = hit.source();
            if (doc == null) {
                continue;
            }
            items.add(toItem(hit.id(), doc));
            nextSearchAfter = hit.sort().stream().map(LogElasticsearchSearcher::fromFieldValue).toList();
        }
        long total = resp.hits().total() != null ? resp.hits().total().value() : 0L;
        // 마지막 페이지(조회된 건수 < size)면 커서 제거
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

    private static co.elastic.clients.elasticsearch._types.FieldValue toFieldValue(Object raw) {
        if (raw instanceof Number n) {
            return co.elastic.clients.elasticsearch._types.FieldValue.of(n.longValue());
        }
        if (raw instanceof String s) {
            return co.elastic.clients.elasticsearch._types.FieldValue.of(s);
        }
        if (raw instanceof Boolean b) {
            return co.elastic.clients.elasticsearch._types.FieldValue.of(b);
        }
        return co.elastic.clients.elasticsearch._types.FieldValue.of(String.valueOf(raw));
    }

    private static Object fromFieldValue(co.elastic.clients.elasticsearch._types.FieldValue fv) {
        return switch (fv._kind()) {
            case Long -> fv.longValue();
            case Double -> fv.doubleValue();
            case String -> fv.stringValue();
            case Boolean -> fv.booleanValue();
            default -> fv._toJsonString();
        };
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