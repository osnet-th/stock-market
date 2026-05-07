---
title: "한국 공공 Open API 다중 출처 어댑터 — envelope/인증/포맷 함정"
category: architecture-patterns
date: 2026-05-07
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

## Trade-offs

- **출처별 DTO 분리** vs **공통 DTO 1개**: 분리 채택 — 각 출처 응답 변동의 폭발 반경이 자기 폴더 안에 갇힘. 단점은 코드 중복(envelope 재선언) 이지만, data.go.kr 계열은 `MolitApiResponse` 1개를 4개 출처가 재사용해서 완화.
- **`@ConditionalOnProperty`** vs **fetch 시점 IllegalStateException**: 후자 채택 — 운영 가시성과 부팅 안정성을 위해.
- **하드코드 통계표 ID** vs **properties 분리**: properties 분리 — 운영 시 명세 변경에 빠르게 대응. 단점은 yml 파일이 커짐.

## Verification

본 패턴 채택 후 부동산 도메인 일배치(06:00 KST) 가 다음을 만족하는지 확인:

- 인증키 1개만 활성한 상태로 부팅 → 활성 출처만 history INSERT, 비활성 출처는 `log.debug + skip`.
- 한 출처의 RestClient timeout (예: KOSIS 정전) → 해당 항목만 격리(captureExecutor) → 다른 출처/지역 진행.
- T11 출처 탭에 8개 출처 모두 표시됨 (lastSuccessAt 가 null이면 미설정/실패 상태로 명시).

## See Also

- `docs/solutions/architecture-patterns/external-http-per-item-transaction-isolation-2026-04-26.md`
- `docs/solutions/architecture-patterns/global-indicator-history-mirroring.md`
- `docs/solutions/performance-issues/parallel-external-fetch-resilience-2026-04-23.md`
- `docs/plans/2026-05-06-001-feat-real-estate-market-data-plan.md`