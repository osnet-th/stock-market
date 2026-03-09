# KIS Access Token 관리 설계

## 작업 리스트

- [x] KisProperties 작성 (`@ConfigurationProperties`)
- [x] KisTokenResponse DTO 작성
- [x] KisApiException 작성
- [x] KisTokenManager 작성 (토큰 발급 + Caffeine 캐시)
- [x] application.yml에 KIS 설정 추가 (appkey, appsecret)
- [x] .env.example에 환경변수 추가

## 배경

한국투자증권 Open API 호출 시 Access Token이 필요하다. 토큰은 1일 유효하며, 모든 API 요청 헤더에 포함해야 한다. 토큰 관리를 infrastructure 계층에 캡슐화하여 application/domain 계층이 토큰 존재를 모르게 한다.

## 핵심 결정

- **위치**: `stock/infrastructure/stock/kis/` 하위 — 기존 `datagokr/` 패턴과 동일 레벨
- **토큰 캐싱**: Caffeine 캐시 사용 (TTL 23시간, 토큰 유효기간 24시간보다 여유 확보)
- **KisTokenManager**: `@Component`로 등록, 향후 KIS API 클라이언트가 주입받아 사용
- **투명한 토큰 제공**: `getAccessToken()` 호출 시 캐시 히트면 즉시 반환, 미스면 자동 발급
- **application/domain 노출 금지**: 토큰 관리는 infrastructure 내부에서만 처리

## 패키지 구조

```
stock/infrastructure/stock/kis/
├── config/
│   └── KisProperties.java          # appkey, appsecret, base-url
├── dto/
│   └── KisTokenResponse.java       # 토큰 발급 응답 DTO
├── exception/
│   └── KisApiException.java        # KIS API 예외
└── KisTokenManager.java            # 토큰 발급 + 캐시 관리
```

## 구현

### KisProperties

위치: `stock/infrastructure/stock/kis/config/KisProperties.java`

기존 `DataGoKrProperties` 패턴과 동일. prefix: `kis.api`

[예시 코드](./examples/infrastructure-config-example.md)

### KisTokenResponse

위치: `stock/infrastructure/stock/kis/dto/KisTokenResponse.java`

KIS `/oauth2/tokenP` 응답 매핑 DTO.

[예시 코드](./examples/infrastructure-dto-example.md)

### KisApiException

위치: `stock/infrastructure/stock/kis/exception/KisApiException.java`

기존 `DataGoKrApiException` 패턴과 동일.

[예시 코드](./examples/infrastructure-exception-example.md)

### KisTokenManager

위치: `stock/infrastructure/stock/kis/KisTokenManager.java`

- Caffeine 캐시를 직접 생성하여 토큰 관리 (Spring `@Cacheable` 대신 프로그래매틱 캐시)
- `getAccessToken()`: 캐시 히트 시 즉시 반환, 미스 시 REST API 호출로 토큰 발급
- 토큰 발급 실패 시 `KisApiException` throw

[예시 코드](./examples/infrastructure-token-manager-example.md)

### application.yml 설정

```yaml
kis:
  api:
    base-url: https://openapi.koreainvestment.com:9443
    app-key: ${KIS_APP_KEY}
    app-secret: ${KIS_APP_SECRET}
```

## 주의사항

- 토큰 발급 API는 **1분당 1회** 제한 — Caffeine 캐시 TTL(23h)로 중복 발급 방지
- 6시간 이내 재발급 요청 시 기존 토큰이 유지됨 (KIS 서버 측 동작)
- 모의투자 환경 분기는 현재 범위에 포함하지 않음 (향후 필요 시 확장)