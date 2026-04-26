# [stock/kis] KisRestClientConfig 신설 + connect/read timeout (옵션 A)

> 분석: [kis-restclient-timeout](../../../analyzes/stock/kis-restclient-timeout/kis-restclient-timeout.md). plan task: Phase 10 P1 #4.

## 의도

KIS 전용 RestClient 빈을 신설해 connect/read timeout 명시. KisApiClient/KisTokenManager 가 `@Qualifier("kisRestClient")` 로 주입. KIS 응답 지연이 stocknoteSnapshotExecutor / Tomcat 워커로 전파되지 않도록 차단.

## 변경 사항

### 1. `KisProperties` 에 timeout 필드 추가

```java
@Getter @Setter
@Component
@ConfigurationProperties(prefix = "kis.api")
public class KisProperties {

    private String url;
    private String key;
    private String secret;
    private String account;
    private Master master = new Master();

    /** RestClient connect timeout (ms). 기본 3000. */
    private long connectTimeoutMs = 3000L;

    /** RestClient read timeout (ms). 기본 10000. */
    private long readTimeoutMs = 10000L;

    @Getter @Setter
    public static class Master {
        private String baseUrl;
    }
}
```

### 2. 신규 `KisRestClientConfig`

위치: `stock/infrastructure/stock/kis/config/KisRestClientConfig.java`

```java
@Configuration
@RequiredArgsConstructor
public class KisRestClientConfig {

    private final KisProperties properties;

    @Bean("kisRestClient")
    public RestClient kisRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
        return RestClient.builder().requestFactory(factory).build();
    }
}
```

### 3. `KisApiClient` 주입 변경

```java
@Component
public class KisApiClient {
    private final RestClient restClient;
    private final KisProperties properties;
    private final KisTokenManager tokenManager;

    public KisApiClient(@Qualifier("kisRestClient") RestClient restClient,
                        KisProperties properties,
                        KisTokenManager tokenManager) {
        this.restClient = restClient;
        this.properties = properties;
        this.tokenManager = tokenManager;
    }
    // 나머지 동일
}
```

기존 `@RequiredArgsConstructor` (Lombok) 는 `@Qualifier` 를 자동 생성자에 못 붙이므로 수동 생성자로 변경.

### 4. `KisTokenManager` 주입 변경

```java
@Component
public class KisTokenManager {
    private final RestClient restClient;
    private final KisProperties properties;

    public KisTokenManager(@Qualifier("kisRestClient") RestClient restClient,
                           KisProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }
    // 나머지 동일
}
```

### 5. `application.yml` (선택, 기본값으로 동작 가능)

```yaml
kis:
  api:
    connect-timeout-ms: 3000
    read-timeout-ms: 10000
```

미설정이어도 KisProperties 의 default 값 (3000/10000) 사용. 운영 환경에서 조정 가능.

## 영향 범위

| 영향 받는 빈 | 동작 |
|---|---|
| KisApiClient | kisRestClient 사용 — 모든 KIS API 호출에 timeout 적용 |
| KisTokenManager | kisRestClient 사용 — 토큰 발급에도 timeout 적용 |
| 기존 default `restClient` 빈 | 변경 없음 — News, ECOS, KoreaExim 등 그대로 사용 |
| DART, SEC | 자기 빈 사용 — 변경 없음 |

## 회귀 위험

| 위험 | 영향 | 완화 |
|---|---|---|
| 정상 응답이 read timeout 초과 (>10s) | KisApiException 발생 → captureTarget catch → markFailed → retryCount++ | 운영 모니터링 후 timeout 값 조정 가능 (application.yml) |
| KisApiClient/KisTokenManager 의 @Qualifier 누락으로 default 빈 주입 회귀 | KIS 가 timeout 없는 빈 다시 사용 | 컴파일러 검증 + 코드 리뷰 |
| `@RequiredArgsConstructor` 제거로 생성자 변경 — Lombok 의존 일관성 | 본 두 빈만 수동 생성자 | DART/SEC 와 동일 패턴 (DartApiClient, SecApiClient 모두 수동 생성자) |

## 작업 리스트

- [ ] `KisProperties.java` connectTimeoutMs / readTimeoutMs 필드 추가
- [ ] `KisRestClientConfig.java` 신설
- [ ] `KisApiClient.java` `@RequiredArgsConstructor` 제거 + 수동 생성자 + `@Qualifier("kisRestClient")`
- [ ] `KisTokenManager.java` 동일
- [ ] 컴파일 확인
- [ ] (선택) `application.yml` 에 kis.api.connect-timeout-ms / read-timeout-ms 명시
- [ ] plan checkbox 갱신 (P1 #4)

## 승인 대기

태형님 승인 후 구현 진행.