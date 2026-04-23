---
title: Spring Data ES bulkIndex로 저장한 문서를 ES Java API Client로 읽을 때 _class 필드로 인한 역직렬화 실패
category: integration-issues
date: 2026-04-21
tags: [elasticsearch, spring-data-elasticsearch, java-api-client, deserialization, _class, search_after, es9, fielddata, sort, pagination, logging]
module: logging
problem_type: integration
severity: HIGH
symptom: LogSearchService 호출 시 모든 검색 응답이 "Failed to decode response"로 실패하여 빈 결과만 반환되고, 수정 후에도 _id 정렬 및 search_after 페이지네이션에서 연쇄 오류 발생
root_cause: Spring Data Elasticsearch가 bulkIndex 시 _source에 자동 주입하는 _class 타입 힌트 필드를 raw ElasticsearchClient의 Jackson 디시리얼라이저가 알지 못해 역직렬화 실패. 추가로 ES 9의 _id fielddata 기본 비활성화 및 search_after 값 타입 불일치(String vs Long/Integer) 문제 동반
affected_versions: Spring Boot 4 + Spring Data Elasticsearch 6.x + Elasticsearch Java API Client 9.x + Elasticsearch 9.x
---

## Symptom

운영 환경의 애플리케이션 로그 뷰어에서 Elasticsearch 에 적재된 로그 도큐먼트가 UI 상에서 항상 빈 리스트로 표시되는 현상이 발생했습니다. Raw Java API Client 로 검색을 호출하면 HTTP 200 을 받고도 다음과 같은 예외로 즉시 실패했습니다.

```
co.elastic.clients.transport.TransportException:
  node: ..., status: 200, [es/search] Failed to decode response
Caused by: com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException:
  Unrecognized field "_class" (class ApplicationLogDocument),
  not marked as ignorable
```

응답 래퍼는 `SearchResponse{took=..., hits=HitsMetadata{total=..., hits=[]}}` 형태로 `total.value` 는 정상(수천 건)이지만 `hits` 배열은 항상 비어 있었습니다. 반면 동일한 인덱스 대상으로 날짜 버킷 aggregation 은 정상 동작했습니다.

## Root Cause

로그 write 경로는 `ElasticsearchOperations.bulkIndex(...)` 로 구현되어 있었는데, Spring Data Elasticsearch 는 기본 설정에서 `EntityMapper` 를 통해 `_source` 에 타입 힌트 필드를 자동 주입합니다.

```json
{
  "timestamp": "2026-04-20T...",
  "level": "INFO",
  "message": "...",
  "_class": "com.thlee.stockmarket.log.document.ApplicationLogDocument"
}
```

이 `_class` 필드는 다형적 역직렬화 용도의 메타데이터로, Spring Data ES 는 read 시 이를 소비하고 애플리케이션 필드에서 제거합니다. 그러나 Raw `co.elastic.clients.elasticsearch.ElasticsearchClient` 가 사용하는 Jackson `ObjectMapper` 는 이 규약을 모르며, `FAIL_ON_UNKNOWN_PROPERTIES=true` 기본값 하에서 `_class` 를 만나면 전체 hit 디코딩을 실패 처리합니다. Aggregation 이 성공한 이유는 doc_values 기반이라 `_source` 디코딩을 거치지 않기 때문입니다.

## Investigation

1. 쿼리 DSL 과 인덱스 mapping 부터 의심했으나, Dev Tools 에서 동일 쿼리 실행 시 올바른 hits 가 반환되어 쿼리 자체는 문제없음 확인.
2. Aggregation(일자별 카운트)은 정상 반환, `hits.total` 도 정상. 오직 `hits.hits[]` 만 비어 반환된다는 비대칭성이 결정적 단서.
3. 전송 레이어 로그 레벨을 DEBUG 로 올려 raw HTTP 응답을 확인, `_class` 필드 존재 확인.
4. Write 경로를 추적하여 Spring Data ES `ElasticsearchOperations.bulkIndex()` 가 해당 필드를 주입하고 있음을 확인.

## Solution

### Before (Raw ES Java Client)

```java
SearchResponse<ApplicationLogDocument> response = elasticsearchClient.search(s -> s
        .index(indexName)
        .query(q -> q.bool(buildBoolQuery(condition)))
        .size(pageSize)
        .sort(so -> so.field(f -> f.field("timestamp").order(SortOrder.Desc)))
        .sort(so -> so.field(f -> f.field("_id").order(SortOrder.Asc)))
        .searchAfter(toFieldValues(cursor)),
    ApplicationLogDocument.class);

return response.hits().hits().stream()
        .map(Hit::source)
        .toList();
```

### After (Spring Data ES `ElasticsearchOperations`)

```java
NativeQueryBuilder qb = NativeQuery.builder()
        .withQuery(buildQuery(req))
        .withMaxResults(size)
        .withSort(SortOptions.of(so -> so.field(f -> f.field("timestamp").order(SortOrder.Desc))))
        .withSort(SortOptions.of(so -> so.field(f -> f.field("_doc").order(SortOrder.Asc))))
        .withTrackTotalHits(true);
if (req.searchAfter() != null && !req.searchAfter().isEmpty()) {
    qb.withSearchAfter(req.searchAfter().stream()
            .map(LogElasticsearchSearcher::coerceSearchAfterValue)
            .toList());
}

SearchHits<ApplicationLogDocument> searchHits = elasticsearchOperations.search(
        qb.build(),
        ApplicationLogDocument.class,
        IndexCoordinates.of(indexPattern));
```

Spring Data ES 는 자신이 주입한 `_class` 를 EntityMapper 경로에서 제거한 뒤 매핑하므로 역직렬화가 정상 동작합니다.

### `_id` 대신 `_doc` 을 tie-breaker 로 사용

ES 9 에서 `_id` 는 fielddata 가 기본 비활성화되어 sort/search_after 키로 사용하면 `Fielddata is disabled on [_id]` 예외가 발생합니다. 결정적 정렬만 필요하므로 `_doc` 으로 대체합니다.

```java
.withSort(SortOptions.of(so -> so.field(f -> f.field("timestamp").order(SortOrder.Desc))))
.withSort(SortOptions.of(so -> so.field(f -> f.field("_doc").order(SortOrder.Asc))))
```

### `search_after` 값 타입 복원

프론트는 search_after 커서를 쿼리스트링으로 전달하면서 모든 값을 String 으로 직렬화합니다. ES 는 sort field 의 원본 타입(`timestamp`=epoch millis long, `_doc`=integer)을 기대하므로 서버에서 복원합니다.

```java
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
```

Long 을 먼저 시도하고, 실패 시 Double, 그래도 실패하면 원본 문자열을 유지하는 3 단계 폴백 구조입니다.

## Why `@JsonIgnoreProperties` Didn't Work

문서 클래스에 다음을 추가해도 증상이 지속되었습니다.

```java
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplicationLogDocument { ... }
```

이 애노테이션은 Jackson 의 `ObjectMapper` 가 `BeanDeserializer` 를 빌드할 때 소비됩니다. 그러나 Elastic 의 Java API Client 는 Spring 컨텍스트의 ObjectMapper 를 재사용하지 않고 `JacksonJsonpMapper` 내부에서 **자체 ObjectMapper 인스턴스**를 구성하며, 이 매퍼의 `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES` 는 기본값 true 로 설정됩니다. 애노테이션은 클래스 스캔 시 읽히긴 하지만, 실제 디코딩 실패는 개별 필드 레벨이 아니라 transport 응답 스트림 전체를 래핑하는 상위 클래스(`SearchResponse`, `HitsMetadata`, `Hit` 등 Elastic 클라이언트 소유 타입) 에서 `source` 를 먼저 raw JSON 으로 읽은 뒤 타깃 타입으로 재매핑하는 과정에서 발생합니다. `Hit.source` 매핑 경로는 애플리케이션 도메인 클래스 애노테이션을 거치긴 하지만, 실패 지점이 그 이전 transport 디코딩 단계라 애노테이션이 영향을 주지 못했습니다.

근본 해법은 "매퍼를 바꿔 `_class` 를 무시"가 아니라 "write 시 필드를 넣은 주체(Spring Data ES)가 read 도 담당하게" 하는 것이었습니다.

## Prevention Strategies

- **한 모듈 = 하나의 ES 접근 경로**: 한 모듈 안에서는 write 와 read 를 반드시 같은 추상화 레벨로 통일합니다. `ElasticsearchOperations` 를 쓰기에 사용했다면 읽기도 `ElasticsearchOperations` 로, raw `ElasticsearchClient` 를 쓰기에 사용했다면 읽기도 raw client 로 일관되게 사용하세요. 두 클라이언트는 직렬화/역직렬화 규약이 달라서 섞어 쓰면 `_class` 타입 힌트 같은 메타데이터 충돌로 조용히 깨집니다.

- **read/write 규약 일치**: Spring Data ES 로 저장한 문서는 반드시 Spring Data ES 로 읽고, raw 로 저장한 문서는 raw 로 읽으세요. 모듈이 커지고 사람이 바뀌면 가장 먼저 무너지는 규칙입니다.

- **Pattern mirroring for new modules**: 새 로깅/검색 모듈을 추가할 때는 이미 안정적으로 동작하는 `news` 모듈(`NewsElasticsearchSearcher`, `NewsElasticsearchIndexer`)의 구조를 그대로 복제하세요. 검색 호출은 `ElasticsearchOperations.search(NativeQuery, class, IndexCoordinates)` 형태를 표준으로.

- **ES 버전 업그레이드 시 sort/field API deprecation 검토**: ES 8.x → 9.x 에서 `_id` sort 가 fielddata 비활성화로 기본 실패합니다. tie-breaker 는 항상 안정적인 `_doc` 또는 `timestamp + _doc` 조합을 쓰세요.

- **Search-after 값은 opaque 금지**: ES 의 `hits[].sort` 값은 long/integer/string 등 원본 타입을 유지합니다. 프론트 왕복 시 문자열이 되므로 서버는 정렬 필드 타입 기반으로 원본 타입을 복원해야 합니다.

- **Graceful degrade 의 독**: 검색 서비스가 모든 ES 예외를 catch 해서 빈 응답을 반환하는 패턴은 UX 에는 좋지만 진단을 마비시킵니다. 최소한 **WARN 로그에 exception cause chain 포함** — `log.warn("es search failed", e)` 가 `log.warn("es search failed: {}", e.getMessage())` 보다 항상 낫습니다.

## Diagnostic Playbook (when this problem recurs)

- 검색 결과 = 0건인데 aggregation = N건 으로 불일치하면 역직렬화 실패 신호. 서버 로그에서 `[es/search] Failed to decode response` 또는 Jackson `UnrecognizedPropertyException` 확인.
- 같은 시간 window 로 ES 에 직접 쿼리 (`curl /_search?q=...`) 하여 실제 문서 존재 여부 검증. 0 이면 인덱싱 문제, N 이면 클라이언트 역직렬화 문제.
- ES 응답의 `hits[].sort` 값 타입 확인. long 인지 string 인지에 따라 `search_after` 복원 로직이 달라집니다. 필드 매핑(`keyword` vs `long`)과 일치하는지 함께 확인.

## Test Case Suggestions

- **Round-trip integration test**: 단일 도큐먼트 write → read round-trip. `_class` 필드가 write 된 뒤 read 시 정상 deserialization 되는지 assertion.
- **Regression assertion**: `_id` 로 sort 를 구성한 쿼리가 즉시 실패하도록 assertion 하여, 실수로 `_id` tie-breaker 를 재도입하면 CI 에서 걸러지게 합니다.
- **Pagination continuity**: `search_after` 연속 호출 최소 2 페이지 이상, 중복 없이 마지막 페이지에서 정상 종료. 정렬 필드 타입이 long / string 두 케이스 모두 커버.

## Related Documentation & References

### Prior Solutions

- [`integration-issues/elasticsearch-localdate-serialization-mismatch-2026-04-20.md`](elasticsearch-localdate-serialization-mismatch-2026-04-20.md) — 직접 연관. Spring Data ES 6.0.x `LocalDateTime/LocalDate` 타입 미스매치로 빈 검색 결과 발생. catch-block-swallowing 안티패턴(`NewsElasticsearchSearcher` try-catch) 이 본 이슈와 동일한 실패 모드.
- [`architecture-patterns/global-indicator-history-mirroring.md`](../architecture-patterns/global-indicator-history-mirroring.md) — 직접 관련 없으나 Spring Data 키워드 관련 문서로 참고.

### Codebase References

- `src/main/java/com/thlee/stock/market/stockmarket/news/infrastructure/elasticsearch/NewsElasticsearchSearcher.java:35-94` — 참조 구현. `ElasticsearchOperations.search(NativeQuery, Class)` (라인 44), `NativeQuery.builder().withQuery(...)` (라인 90-93). 로깅 모듈이 도입한 패턴.
- `src/main/java/com/thlee/stock/market/stockmarket/news/infrastructure/elasticsearch/NewsElasticsearchIndexer.java:21-43` — `ElasticsearchOperations.save(documents)` (라인 36) — write 시 `_class` 를 자동 주입하는 경로.
- `src/main/java/com/thlee/stock/market/stockmarket/logging/infrastructure/elasticsearch/LogElasticsearchSearcher.java:62-104` — 수정된 파일. 라인 67-68 주석이 본 교훈 설명. `_doc` sort 라인 73. `coerceSearchAfterValue` 헬퍼 라인 239-.
- `src/main/java/com/thlee/stock/market/stockmarket/logging/infrastructure/elasticsearch/document/ApplicationLogDocument.java` — `@JsonIgnoreProperties(ignoreUnknown = true)` 는 레거시로 남김. `DateFormat.date_time` 선택은 Spring Data ES #2318 회피.

### External References

- [Spring Data Elasticsearch 6.x — Object Mapping](https://docs.spring.io/spring-data/elasticsearch/reference/elasticsearch/object-mapping.html)
- [Elasticsearch Java API Client](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/index.html)
- [search_after pagination](https://www.elastic.co/guide/en/elasticsearch/reference/current/paginate-search-results.html#search-after)
- [Spring Data ES issue #2318 (epoch_millis serialization bug)](https://github.com/spring-projects/spring-data-elasticsearch/issues/2318)

### Prior commits in this session

- `7be0611` `fix(logging): ES 9 에서 _id sort 불가로 검색 결과 0건 이슈 수정`
- `f724093` `fix(logging): ES 역직렬화 실패 수정 — _class 필드 무시로 검색 결과 0건 해결`
- `a7419d2` `fix(logging): raw ES 클라이언트 → ElasticsearchOperations 로 검색 경로 전환`
- `f1341ac` `fix(logging): 로그 더보기 페이지네이션 동작 실패 수정`