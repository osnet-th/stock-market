---
title: "한국 공공 Open API 다중 출처 어댑터 — envelope/인증/포맷 함정"
category: architecture-patterns
date: 2026-05-07
last_updated: 2026-05-20
module: realestate, scheduler, source-adapter
problem_type: best_practice
component: source_adapter
severity: medium
tags:
  - public-open-api
  - data-go-kr
  - kosis
  - reb
  - subscription-home
  - source-adapter
  - response-envelope
  - api-key-management
  - region-routing
  - waf-blocking
  - content-type-spoofing
  - endpoint-migration
applies_when: "data.go.kr / KOSIS / R-ONE / 시·도 자체 포털 등 다중 한국 공공 Open API를 한 도메인이 통합 어댑터링할 때"
---

# 한국 공공 Open API 다중 출처 어댑터 — envelope/인증/포맷 함정

## Context

부동산 도메인은 단일 외부 API에 의존하는 ECOS·TradingEconomics와 달리 **8개 공공 출처(국토교통부 / 한국부동산원 R-ONE / 건축HUB / KOSIS / 청약홈 / HUG / 서울 열린데이터광장 / 경기도 데이터드림)** 를 동시에 어댑터링한다. 각 출처는 인증 방식·응답 envelope·에러 코드 표준이 모두 달라, 단일 패턴으로 흡수하면 어댑터가 하나 깨질 때마다 다른 출처에 회귀가 번진다.

ECOS adapter (`korea/ecos`) 와 동일한 구조로 시작했더니 다음이 깨졌다:

- 단일 `EcosKeyStatResponse` 같은 강타입 DTO를 모든 출처에 적용 → odcloud `{data:[...]}` 와 서울 `{<datasetId>:{list_total_count, row:[...]}}` 가 다른 형태라 직렬화 실패.
- `serviceKey` 라는 query param 이름이 표준이라고 가정 → R-ONE 은 `KEY`, KOSIS 는 `apiKey`, 서울은 path segment(`/{apiKey}/json/{dataset}/...`) 로 인증.
- `<resultCode>00</resultCode>` 가 성공 표준이라고 가정 → odcloud 는 `currentCount`, KOSIS 는 에러 시 `RESULT.CODE: "INFO-000"`, 서울은 `RESULT.CODE: "INFO-000"` 또는 200 + 빈 row.

## Guidance

**다중 공공 출처 어댑터링 시 다음을 출처별로 분리하고, "공통은 최소"로 둔다.**

### 1) 응답 envelope은 출처별로 다루되, 같은 envelope을 쓰는 출처끼리만 DTO를 공유한다

| Envelope 분류 | 출처 | 공통 DTO 후보 |
|---|---|---|
| data.go.kr 표준 (`response/header/body/items.item`) | 국토부, 건축HUB, HUG, 청약홈 일부 | `MolitApiResponse` 형태 1개 재사용 |
| odcloud (`{data:[], totalCount}`) | 청약홈 일부 (15098905) | 별도 — `data:[]` 직접 파싱 |
| R-ONE (`{SttsApiTblData:[...]}`) | 한국부동산원 | 단순 `Map<String,Object>` 로 받고 어댑터에서 정규화 |
| KOSIS (`[{ITM_ID, DT, ...}, ...]` 직접 배열) | KOSIS | `List<Map>` 직접 |
| 서울 열린데이터광장 (`{<datasetId>:{list_total_count, row:[...]}}`) | 서울 | dataset id 가 응답 키이므로 `Map` 으로 받고 키 lookup |
| 경기 데이터드림 (`{<datasetId>:[..., {row:[...]}]}`) | 경기도 | 중첩 위치가 출처마다 다름 → 어댑터 안에서 명시적 traverse |

**금지**: 모든 출처를 강타입 DTO 1개로 흡수하려고 시도하지 말 것. 한 출처의 응답 변동이 다른 출처 deserialization 까지 깨트린다.

### 2) 인증키 미설정은 부팅을 막지 말고, fetch 시점에 IllegalStateException

`@ConditionalOnProperty` 로 어댑터 빈 등록을 막으면 아래 두 가지가 깨진다:

- **운영 가시성**: factory.getAll() 결과에서 누락 → T11 출처 탭에 "비활성" 출처가 안 보임. 사용자가 "왜 이 출처는 데이터가 없나요?" 알 수 없음.
- **부팅 의존**: 인증키 발급 전 배포 시 빈 자체가 누락되어 컴포넌트 그래프가 비대칭. 일배치 스케줄러가 `factory.getAll()` 으로 순회하면 의도치 않은 출처가 빠진다.

**권장**: 어댑터 빈은 항상 등록하고, `fetch()` 진입 시점에 `apiKey` 확인 후 미설정이면 `IllegalStateException`. 스케줄러는 catch 후 `log.debug + return 0` 로 항목 단위 skip.

```java
if (props.getApiKey() == null || props.getApiKey().isBlank()) {
    throw new IllegalStateException("XXX api-key not configured");
}
```

### 3) Region prefix로 동적 활성/비활성 (지역 특화 출처)

서울 열린데이터광장은 `region.value().startsWith("11")` 인 시군구만 의미가 있다. 경기 데이터드림은 `41`. supportedCategories 에서 `SEOUL_LOCAL` / `GYEONGGI_LOCAL` 카테고리를 분리하고, 어댑터 fetch 안에서:

```java
if (!region.isSeoul()) {
    return FetchResult.success(List.of()); // 빈 결과 + 정상 종료
}
```

**금지**: 인증키 없음과 region 미지원을 같은 예외로 묶지 말 것. 운영 noise 가 폭증한다.

### 4) items.item 단일/배열 혼재는 envelope 레벨에서 정규화

data.go.kr 응답은 결과가 1건이면 `items.item: {...}`, N건이면 `items.item: [{...}, {...}]`. Jackson `ACCEPT_SINGLE_VALUE_AS_ARRAY` 에 의존하지 말고 envelope DTO에서 직접 정규화한다.

```java
public List<Map<String, Object>> asList() {
    if (item == null) return List.of();
    if (item instanceof List<?> list) return (List<Map<String, Object>>) list;
    if (item instanceof Map<?, ?> map) return List.of((Map<String, Object>) map);
    return List.of();
}
```

이유: 다른 envelope(R-ONE, KOSIS, 서울/경기) 에 같은 Jackson 옵션이 부정확하게 영향 줄 수 있다. 글로벌 옵션 대신 envelope 단위 정규화.

### 5) 통계표 코드/항목 코드는 yml + properties 의 두 단계로 분리

R-ONE STATBL_ID, KOSIS orgId+tblId+itmId 같은 코드는:

- **yml** (`realestate-indicator-metadata.yml`) — 카테고리×출처×지표 메타. 표시명/설명/단위.
- **properties** — 통계표 ID 매핑 (운영 환경별 변경 가능).

어댑터 안에 하드코딩하면 새 통계표 추가 시 코드 변경 + 재배포 필요. 운영 시 명세 변경에 빠르게 대응하려면 properties 분리.

### 6) 트래픽 한도 초과는 출처별 시간대 분산으로 흡수

8개 출처 × 56시군 × N카테고리 = 한 batch 에 수만 호출. 일부 출처(특히 KOSIS, 청약홈)는 일일 1만 회 한도가 있다. 한도 초과 시:

- **단기**: bounded executor (`core 4 / max 8 / queue 200, AbortPolicy`) 로 자체 throttle.
- **장기**: 출처별 cron 시간대 분산 (`molit 06:00`, `reb 06:10`, `kosis 06:20` ...). Plan에서는 단일 cron 으로 시작하고, 한도 도달 시 분산.

**금지**: `Thread.sleep` 으로 RPS 제어 — 풀 타임아웃과 충돌해 timeout exception 양산.

### 7) 인코딩 — UTF-8을 가정하지 말 것

서울 열린데이터광장 일부 dataset 은 EUC-KR 응답을 반환한다. RestClient default 는 UTF-8 → 한글 깨짐. 출처별로 `Content-Type` 응답 헤더 또는 dataset 메타에서 인코딩 확인 후 명시:

```java
restClient.get()
    .uri(...)
    .accept(MediaType.APPLICATION_JSON)
    // EUC-KR 응답 시: messageConverters에 명시적 charset 지정
    .retrieve()
    .body(...)
```

운영 시 한 번 깨지면 RestClient 전역 설정으로 강제 UTF-8 + EUC-KR 출처는 어댑터 단위 override.

## Operational Findings (2026-05-20 운영 dry-run)

위 1~7번은 plan 작성 시점의 가설. 본 sprint(2026-05-12 ~ 05-20)에서 8회 dry-run을 거치며 추가로 확정된 함정 9개. **다음 공공 API 어댑터를 작성하는 사람은 1~7번 위에 이 9개를 먼저 검토하면 디버깅 시간이 큰 폭으로 단축된다.**

### 8) WAF가 빈 User-Agent를 HTTP 400으로 차단 (data.go.kr)

**현상**: data.go.kr의 모든 endpoint(MOLIT/HUB/HUG/odcloud)가 인증키 정상에도 HTTP 400 응답:
```html
<TITLE>400 Bad Request</TITLE>
<H1>Request Blocked</H1>
```
**원인**: Spring `RestClient` default가 User-Agent 헤더를 보내지 않거나 비표준 형식 → data.go.kr 게이트웨이 WAF가 사전 차단.
**해결**: RestClient bean에 default User-Agent 1줄 추가하면 모든 어댑터 일괄 적용:
```java
return RestClient.builder()
    .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
    .defaultHeader("User-Agent", "stock-market-realestate/1.0 (Java RestClient)")
    .build();
```
**금지**: 어댑터별 헤더 추가는 누락 위험. RestClient bean 단일 지점에서 처리.

### 9) Endpoint deprecation은 신규 path가 안내 HTML로 알려준다 (KOSIS)

**현상**: KOSIS `/openapi/statisticsParameterData.do` 호출 시 HTTP 404 + HTML "국가통계포털 서비스 개편에 따라 새 주소로 변경" 안내 페이지. 5초 후 `/openapi/index.jsp`로 redirect.
**해결**: 신규 path는 `/openapi/Param/statisticsParameterData.do` (Param 경로 추가).
**진단 패턴**: 정부 API가 deprecated되면 보통 5초 redirect HTML 안내가 표준. Content-Type이 `text/html`이고 본문에 "주소로 변경" / "개편" 같은 키워드 있으면 endpoint 마이그레이션 의심.

### 10) items 빈 문자열 — totalCount=0 시 객체 대신 `""` 반환 (MOLIT)

**현상**: 강남구는 `items:{item:[...]}` 정상, 수원 장안구(데이터 없음)는 `items:""` (빈 문자열) 반환. Jackson이 String → Items 객체 매핑 실패로 RestClient `Error while extracting response`.
**해결**: DTO `Body.items` 타입을 `Object`로 받고 `rows()`에서 String/Map 분기:
```java
public static class Body {
    private Object items; // String("") | Map{item:[...]} | null
    // ...
}
public List<Map<String, Object>> rows() {
    Object items = response.body.items;
    if (items == null || items instanceof String) return Collections.emptyList();
    if (items instanceof Map<?, ?> itemsMap) { /* item 분기 */ }
    return Collections.emptyList();
}
```
**금지**: Items 강타입 클래스 + `ACCEPT_SINGLE_VALUE_AS_ARRAY` 조합 — 빈 응답 케이스를 못 잡음.

### 11) resultCode 2자리 vs 3자리 혼용 (MOLIT)

**현상**: 같은 data.go.kr 표준이라도 endpoint에 따라 `"00"`(2자리) 또는 `"000"`(3자리). isSuccess() 검증을 `"00".equals(code)` 한쪽만 하면 정상 응답도 실패로 분류.
**해결**: 양쪽 모두 정상으로 처리:
```java
public boolean isSuccess() {
    String code = response.header.resultCode;
    return "00".equals(code) || "000".equals(code);
}
```

### 12) Content-Type이 `text/html`이어도 본문은 JSON (KOSIS, GG)

**현상**: KOSIS / 경기데이터드림이 정상 JSON 응답을 Content-Type `text/html;charset=UTF-8`로 보냄. Spring RestClient `.body(List.class)` 또는 `.body(Map.class)`는 Content-Type 기반 매핑이라 실패: `Could not extract response: no suitable HttpMessageConverter found for response type [Map] and content type [text/html;charset=UTF-8]`.
**해결**: `.body(String.class)`로 본문을 raw String으로 받고 `ObjectMapper`로 직접 파싱:
```java
String body = restClient.get().uri(...).retrieve().body(String.class);
List<Map<String, Object>> rows = parseRows(body);

private List<Map<String, Object>> parseRows(String body) {
    if (body == null || body.isBlank()) return Collections.emptyList();
    if (body.trim().startsWith("{") && body.contains("\"err\"")) {
        return Collections.emptyList(); // 에러 envelope
    }
    return objectMapper.readValue(body, new TypeReference<>() {});
}
```
**일반화**: 공공 API의 Content-Type은 신뢰 불가 — 본문 첫 글자(`[`/`{`)로 JSON 여부 판단이 더 안전.

### 13) 시점 기준은 발표 lag 큰 통계에서 위험 — `newEstPrdCnt`가 안정 (KOSIS)

**현상**: KOSIS 미분양 통계는 발표 lag 1~2개월. `startPrdDe=202604&endPrdDe=202605` 호출 시 `{err:"30","데이터가 존재하지 않습니다"}` 응답. 일배치가 항상 빈 응답 받음.
**해결**: 시점기준 대신 최근 N개월 기준 사용:
```java
.queryParam("newEstPrdCnt", 3)  // 최근 3개월 자동 선택
// (startPrdDe/endPrdDe 제거)
```
KOSIS 가이드 명세: "시점기준 또는 최신자료기준 택1".
**일반화**: 통계 출처는 발표 lag을 모를 때 `newEstPrdCnt` 안정. 실거래 출처(MOLIT)는 lag이 작아 시점기준 OK.

### 14) 한글 시도명 약식 vs 정식 표기 차이 (KOSIS)

**현상**: KOSIS 응답 `C1_NM`은 약식 한글 ("서울", "경기", "강원"). DB region entity의 `sidoName`은 정식 행정구역명 ("서울특별시", "경기도", "강원특별자치도"). 직접 비교 시 매칭 0건.
**해결**: 매핑 테이블 1개 추가:
```java
private static final Map<String, String> KOSIS_SIDO_TO_OFFICIAL = Map.ofEntries(
    Map.entry("서울", "서울특별시"),
    Map.entry("경기", "경기도"),
    Map.entry("강원", "강원특별자치도"),
    Map.entry("전북", "전북특별자치도"),
    // ... 17개 시도
);
```
**일반화**: 공공 API의 지역명은 출처마다 표기 다름. region 매칭은 한글명보다 행정구역코드(SIGUNGU_CODE 5자리) 기반이 안전하나, 출처가 코드를 안 주는 경우 매핑 테이블 1개 추가.

### 15) contextPath 누락 시 호스트 홈페이지 404로 떨어진다 (REB)

**현상**: REB 호출 시 응답이 REB 홈페이지의 404 페이지: "홈페이지 오류 알림 - 요청하신 페이지가 정상적으로 처리되지 않았습니다... `/reb/main.do`로 이동". 즉 endpoint 자체가 매핑 안 되어 호스트의 기본 404 핸들러로 떨어짐.
**원인**: baseUrl이 `https://www.reb.or.kr/r-one/openapi`인데 어댑터가 contextPath(`/r-one/openapi`)를 합치지 않고 `/SttsApiTblData.do` 만으로 호출 → 실제 URL `https://www.reb.or.kr/SttsApiTblData.do` → 404.
**해결**: BaseUri 파싱 후 contextPath 명시적 합산:
```java
BaseUri base = BaseUri.parse(props.getBaseUrl());
.path(base.contextPath() + endpointPath)  // contextPath 합치기
```
**일반화**: 호스트 홈페이지의 404 HTML이 응답으로 오면 endpoint path 누락 의심. baseUrl이 path 일부를 포함하는 경우 BaseUri parsing으로 명시.

### 16) 부분 실패 허용 — 한 어댑터가 다수 endpoint 호출 시 첫 실패에 throw 금지

**현상**: 경기데이터드림 어댑터가 4개 dataset 순차 호출. 첫 dataset의 `RestClientException`을 `throw new GgDataDreamApiException(...)`로 처리 → for loop 종료 → 나머지 3개 dataset 호출 시도 안 됨. SSL handshake 일시 실패 1건이 전체 region을 막음.
**해결**: try-catch + continue 패턴:
```java
List<RealEstateMarketIndicator> indicators = new ArrayList<>();
for (Dataset ds : Dataset.values()) {
    try {
        indicators.addAll(callDataset(ds, region));
    } catch (RestClientException e) {
        log.warn("[gg] dataset fetch failed: {}, cause={}", ds.path,
                SecretMasker.sanitize(e).getMessage());
        // continue — 다른 dataset 호출은 계속
    }
}
return FetchResult.success(indicators);  // 부분 적재
```
**금지**: 첫 실패에 throw — 일시 네트워크 이슈가 전체 region의 데이터 누락으로 번짐.

### 17) 진단성 — 4xx/5xx 응답 body snippet을 caused by에 자동 포함

**현상**: 어댑터 catch에서 `throw new XxxApiException(msg, cause)`로 wrap 시 cause의 message만 보존. 4xx/5xx 응답 본문(에러 안내)은 stacktrace에 안 들어가 디버깅 시 별도 호출 재현이 필요.
**해결**: `SecretMasker.sanitize()`에 `RestClientResponseException`일 때 status + body 300자 자동 포함:
```java
public static Throwable sanitize(Throwable original) {
    String message = String.valueOf(original.getMessage());
    if (original instanceof RestClientResponseException rcre) {
        String body = String.valueOf(rcre.getResponseBodyAsString()).replaceAll("\\s+", " ");
        String snippet = body.length() > 300 ? body.substring(0, 300) + "...(truncated)" : body;
        message = String.format("HTTP %d | %s | body: %s",
                rcre.getStatusCode().value(), message, snippet);
    }
    Throwable copy = new RuntimeException(mask(message));
    copy.setStackTrace(original.getStackTrace());
    return copy;
}
```
이제 운영 로그의 `Caused by: java.lang.RuntimeException: HTTP 404 | ... | body: <!DOCTYPE html>...홈페이지 오류 알림...`로 root cause 즉시 식별 가능. 동일 호출 재현 없이 endpoint 잘못/응답 형식 다름/인증 거부를 한 번에 분류.

## 다음 어댑터 작성자를 위한 체크리스트 (TL;DR)

새 한국 공공 Open API 어댑터를 작성한다면 **이 순서로** 검증:

1. RestClient bean에 default User-Agent 있는지 (#8)
2. 응답 Content-Type 무시하고 `body(String.class)` + ObjectMapper로 받기 (#12)
3. 빈 응답이 `""`(문자열) / `null` / `{err:...}` / `{data:[]}` 중 어떤 형태인지 직접 호출로 확인 (#10, #13)
4. resultCode 2자리/3자리 혼용 가능성 (#11)
5. baseUrl에 contextPath 포함되어 있으면 `BaseUri.parse` + 명시적 합산 (#15)
6. 지역 매칭은 한글명보다 코드 우선, 한글명 매칭 시 시도명 매핑 테이블 (#14)
7. 다수 endpoint 호출 시 try-catch + continue 패턴 (#16)
8. catch wrap 시 `SecretMasker.sanitize()`로 body snippet 포함 (#17)
9. endpoint 404 응답이 호스트 홈페이지 HTML이면 deprecated/마이그레이션 의심 (#9, #15)

## Trade-offs

- **출처별 DTO 분리** vs **공통 DTO 1개**: 분리 채택 — 각 출처 응답 변동의 폭발 반경이 자기 폴더 안에 갇힘. 단점은 코드 중복(envelope 재선언) 이지만, data.go.kr 계열은 `MolitApiResponse` 1개를 4개 출처가 재사용해서 완화.
- **`@ConditionalOnProperty`** vs **fetch 시점 IllegalStateException**: 후자 채택 — 운영 가시성과 부팅 안정성을 위해.
- **하드코드 통계표 ID** vs **properties 분리**: properties 분리 — 운영 시 명세 변경에 빠르게 대응. 단점은 yml 파일이 커짐.

## Verification

본 패턴 채택 후 부동산 도메인 일배치(06:00 KST) 가 다음을 만족하는지 확인:

- 인증키 1개만 활성한 상태로 부팅 → 활성 출처만 history INSERT, 비활성 출처는 `log.debug + skip`.
- 한 출처의 RestClient timeout (예: KOSIS 정전) → 해당 항목만 격리(captureExecutor) → 다른 출처/지역 진행.
- T11 출처 탭에 8개 출처 모두 표시됨 (lastSuccessAt 가 null이면 미설정/실패 상태로 명시).

### 2026-05-20 운영 dry-run 검증 결과 (8회 batch)

8개 출처 중 6개 정상 (HUB/HUG는 의도된 비활성화):

| Source | 적재 row | 검증된 함정 |
|---|---|---|
| MOLIT | 336 | #8(WAF), #10(빈 문자열), #11(resultCode 혼용) |
| GG_DATA_DREAM | 248 | #12(Content-Type), #15(endpoint 변경), #16(부분 실패) |
| SUBSCRIPTION_HOME | 168 | endpoint placeholder 정정 |
| KOSIS | 56 | #9(deprecated), #12(Content-Type), #13(newEstPrdCnt), #14(시도명) |
| SEOUL_OPEN_DATA | 50 | https 미지원 → http (재발견) |
| REB | 0 | #15(contextPath) — 호출 정상화, 통계표 ID 매핑 별도 |

batch 메트릭: `success=616, failure=0, savedRows=585, elapsedMs=80,494`

## See Also

- `docs/solutions/architecture-patterns/external-http-per-item-transaction-isolation-2026-04-26.md`
- `docs/solutions/architecture-patterns/global-indicator-history-mirroring.md`
- `docs/solutions/performance-issues/parallel-external-fetch-resilience-2026-04-23.md`
- `docs/plans/2026-05-06-001-feat-real-estate-market-data-plan.md`
- `docs/plans/2026-05-10-001-refactor-realestate-review-findings-plan.md` (Operations Follow-up 섹션)
- `docs/brainstorms/2026-05-17-reb-adapter-redesign-requirements.md` (REB 어댑터 재설계 후속)