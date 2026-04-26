# [stock/kis] KIS RestClient connect/read timeout 미설정

> ce-review 2026-04-25 P1 #4. KIS 응답 지연 시 stocknoteSnapshotExecutor 워커 점거 + 카스케이드 영향.

## 현재 상태

### KIS 가 news 모듈 공용 RestClient 빈을 주입받음

`news/.../RestClientConfig.java`

```java
@Bean
public RestClient restClient() {
    return RestClient.builder().build();   // ← timeout 미설정
}
```

`KisApiClient.java:27` `KisTokenManager.java:27` 모두 `@Qualifier` 없이 default `RestClient` 주입 — news 모듈 빈을 받는다.

### 다른 외부 어댑터는 별도 빈 + Qualifier 패턴

| 어댑터 | 빈 | Qualifier |
|---|---|---|
| DART | `@Bean("dartRestClient")` (`DartRestClientConfig`) | `@Qualifier("dartRestClient")` |
| SEC | `@Bean("secRestClient")` (`SecRestClientConfig`) | `@Qualifier("secRestClient")` |
| KIS | ❌ (default) | ❌ |
| Kakao | RestClient.builder().build() (KakaoClient 내부) | n/a — 별도 인스턴스 |

KIS 만 timeout 정책이 누락된 채 default 빈 사용.

## 영향 범위

| 시나리오 | 동작 |
|---|---|
| KIS API 응답 지연 (수십 초 이상) | RestClient 무한 대기 |
| stocknoteSnapshotExecutor (core 4 / max 8 / queue 200) 워커가 KIS 호출 중 | 워커 풀 점거 |
| 풀 포화 + CallerRunsPolicy | Tomcat 워커가 동기 KIS 호출 → 사용자 응답 latency spike |
| KisTokenManager 토큰 발급 호출 (synchronized) 응답 지연 | 모든 KIS 호출 직렬 대기 (Task #33 별건) |

## 해결 옵션

### 옵션 A — KisRestClientConfig 신설 + Qualifier (권장)

DART/SEC 패턴 일관. KisProperties 에 timeout 필드 추가 → `application.yml` 외부화.

```java
@Bean("kisRestClient")
public RestClient kisRestClient(KisProperties properties) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
    factory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
    return RestClient.builder().requestFactory(factory).build();
}
```

`KisApiClient`, `KisTokenManager` 에 `@Qualifier("kisRestClient")` 추가.

| 장점 | 단점 |
|---|---|
| DART/SEC 와 일관 | 신규 config 1개 |
| KIS 특수 timeout 값 적용 가능 | `application.yml` 변경 (kis.api.connect-timeout-ms / read-timeout-ms) |
| 다른 모듈 영향 없음 | |

### 옵션 B — 공용 RestClientConfig 에 timeout 추가

기존 default `restClient` 빈에 timeout 적용. KIS, News(Naver/GNews/NewsAPI), ECOS, KoreaExim 모두 동일 timeout 적용.

| 장점 | 단점 |
|---|---|
| 변경 1 곳 | 다른 모듈도 영향 받음 — 각 외부 API 의 적정 timeout 이 다를 수 있음 |
| | KIS 특수 timeout 못 잡음 (예: 일봉은 더 긴 timeout 필요할 수도) |

## 추천: 옵션 A

근거:
- DART/SEC 어댑터의 검증된 패턴 일관
- KIS 특수 timeout 값 (예: read 10s) 적용 가능
- 다른 모듈 무영향
- KisProperties 에 timeout 필드 추가로 운영 환경별 조정 가능

## Timeout 값 결정

| 항목 | 값 | 근거 |
|---|---|---|
| connect-timeout-ms | 3000 | KIS 서버 연결 정상 시 < 1초. 3초 안전 마진. |
| read-timeout-ms | 10000 | 정상 응답 < 500ms, 느린 응답 1~3초, 이상 응답 > 5초. 10초 마지노선. |

향후 운영 환경 모니터링으로 조정 가능 (application.yml 외부화).

## 코드 위치

| 파일 | 변경 |
|---|---|
| `KisRestClientConfig.java` | 신규 — kisRestClient 빈 |
| `KisProperties.java` | connectTimeoutMs / readTimeoutMs 필드 추가 |
| `KisApiClient.java` | 생성자에 `@Qualifier("kisRestClient")` |
| `KisTokenManager.java` | 동일 |
| `application.yml` | `kis.api.connect-timeout-ms: 3000`, `read-timeout-ms: 10000` |

## 후속 task 와의 관계

| Task | 정합 |
|---|---|
| #33 P2 KisTokenManager synchronized + 외부 호출 분리 | 본 task 의 timeout 적용으로 token 발급 지연도 제한됨 (별건 처리) |
| #28 P2 일봉 long TTL 캐시 | 일봉 호출 자체가 줄면 timeout 의 영향 빈도 감소 (별건) |

## 설계 문서

[kis-restclient-timeout](../../../designs/stock/kis-restclient-timeout/kis-restclient-timeout.md)