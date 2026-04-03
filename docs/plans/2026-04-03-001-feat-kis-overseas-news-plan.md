---
title: "feat: KIS 해외주식 뉴스(속보/뉴스종합) 조회 기능"
type: feat
status: active
date: 2026-04-03
origin: docs/brainstorms/2026-04-03-kis-overseas-news-requirements.md
deepened: 2026-04-03
---

# feat: KIS 해외주식 뉴스(속보/뉴스종합) 조회 기능

## Enhancement Summary

**Deepened on:** 2026-04-03
**Sections enhanced:** 8
**Research agents used:** Architecture Strategist, Security Sentinel, Performance Oracle, Pattern Recognition, Code Simplicity, Frontend Races, Best Practices Researcher, Framework Docs Researcher

### Key Improvements
1. **구조 간소화**: YAGNI 원칙 적용 — domain/model 레이어 제거, 매퍼 인라인화로 10+ 파일 → 7 파일로 축소
2. **보안 강화**: 입력 검증(enum/regex), 에러 메시지 새니타이징, 서버 측 Rate Limit 대응 추가
3. **프론트엔드 레이스 컨디션 방지**: generation counter 패턴 적용 (기존 ecos.js/financial.js 패턴 재사용)
4. **RestClient 패턴 확정**: `.toEntity()` + `onStatus()` 조합으로 헤더 접근 및 에러 핸들링

### New Considerations Discovered
- KIS RestClient에 전용 타임아웃 설정이 없음 (P0 수정 필요)
- 토큰 만료 시 단일 재시도 로직 부재 (첫 클릭 실패 UX 문제)
- KIS 연속조회 시 요청 헤더에도 `tr_cont: "N"` 설정 필요
- 기존 `loadPortfolioNews()`에도 동일한 레이스 컨디션 버그 존재

---

## Overview

포트폴리오 해외주식 종목에서 KIS Open API를 통해 해외속보와 해외뉴스종합을 실시간으로 조회하는 기능을 추가한다. DB 저장 없이 온디맨드 API 호출 방식이며, 기존 키워드 뉴스 시스템과 완전 분리된 별도 도메인으로 구현한다. (see brainstorm: docs/brainstorms/2026-04-03-kis-overseas-news-requirements.md)

## Problem Statement / Motivation

포트폴리오에 해외 주식을 보유한 사용자가 해당 종목의 최신 뉴스와 속보를 빠르게 확인할 수 없다. KIS가 제공하는 해외속보/해외뉴스종합 API를 활용하여 포트폴리오 화면 내에서 종목 맥락의 뉴스를 확인할 수 있도록 한다.

## Proposed Solution

### Backend — `overseasnews` 도메인 신규 생성 (간소화)

기존 `news` 도메인(키워드 기반, DB 저장)과 완전 분리된 `overseasnews` 도메인을 생성한다. DB 저장 없이 KIS API를 직접 호출하여 결과를 반환하는 pass-through 구조이다.

**YAGNI 원칙 적용**: 비즈니스 규칙/영속성이 없는 pass-through 기능이므로, domain/model 레이어를 생략하고 chatbot 도메인 패턴을 따른다. 매퍼는 별도 클래스 대신 서비스에서 인라인 처리한다.

```
overseasnews/
├── application/
│   ├── OverseasNewsService.java           # 유스케이스 (API 호출 → DTO 변환)
│   └── dto/
│       ├── BreakingNewsResponse.java      # 해외속보 응답 DTO
│       └── ComprehensiveNewsResponse.java # 해외뉴스종합 응답 DTO (hasMore 포함)
├── infrastructure/
│   └── kis/
│       ├── KisOverseasNewsClient.java     # KIS 뉴스 API 호출 클라이언트
│       └── dto/
│           ├── KisBreakingNewsOutput.java # 해외속보 API 응답 DTO (@Getter @NoArgsConstructor)
│           └── KisNewsOutput.java         # 해외뉴스종합 API 응답 DTO (@Getter @NoArgsConstructor)
└── presentation/
    └── OverseasNewsController.java        # REST 엔드포인트 (입력 검증 포함)
```

**파일 수: 7개** (기존 계획 10+ 대비 ~35% 감소)

### Research Insights — 구조 간소화 근거

**왜 domain/model을 생략하는가:**
- domain 레이어는 비즈니스 불변식(invariant)을 표현하고 보호하기 위해 존재한다. 이 기능에는 검증할 비즈니스 규칙이 없다
- `OverseasBreakingNews` 도메인 모델과 `BreakingNewsResponse` DTO의 필드가 동일하여 불필요한 중복
- chatbot 도메인이 동일한 pass-through 패턴에서 domain 레이어 없이 `application/port/` → `infrastructure/` 구조를 사용하는 선례 존재 (`chatbot/application/port/LlmPort.java`)
- CLAUDE.md YAGNI 원칙: "현재 설계 문서의 작업 범위에 포함되지 않은 클래스를 미리 만들지 않습니다"

**왜 별도 매퍼 클래스를 생략하는가:**
- 매핑이 단순 필드 복사 (KIS DTO → 응답 DTO)이며, 정확히 1곳에서만 호출됨
- 매퍼를 분리할 정도의 복잡성이나 재사용 필요성이 없음
- 서비스 메서드 내에서 인라인 매핑으로 충분

### KisApiClient 확장 — `getWithContinuation()` 메서드

현재 `KisApiClient.get()` 메서드는 `.retrieve().body()` 패턴으로 응답 헤더를 버린다. 해외뉴스종합 API의 페이지네이션을 위해 `tr_cont` 응답 헤더 접근이 필요하므로, `.toEntity()` 패턴을 사용하는 새 메서드를 추가한다.

```java
// stock/infrastructure/stock/kis/KisApiClient.java에 추가
public <T> KisApiResult<T> getWithContinuation(
        String path, String trId, String trCont,
        Function<UriBuilder, URI> uriFunc,
        ParameterizedTypeReference<KisApiResponse<T>> responseType,
        String description) {

    ResponseEntity<KisApiResponse<T>> entity = restClient.get()
        .uri(properties.getUrl() + path, uriFunc)
        .headers(headers -> {
            headers.setBearerAuth(tokenManager.getAccessToken());
            headers.set("appkey", properties.getKey());
            headers.set("appsecret", properties.getSecret());
            headers.set("tr_id", trId);
            headers.set("tr_cont", trCont);  // "" 또는 "N"
        })
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
            throw new KisApiException(description + " 클라이언트 오류");
        })
        .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
            throw new KisApiException(description + " 서버 오류");
        })
        .toEntity(responseType);  // body() 대신 toEntity()

    KisApiResponse<T> response = entity.getBody();
    // ... 검증 로직 ...

    String responseTrCont = entity.getHeaders().getFirst("tr_cont");
    boolean hasNext = "F".equals(responseTrCont) || "M".equals(responseTrCont);

    return new KisApiResult<>(response.getOutput(), hasNext);
}

// stock/infrastructure/stock/kis/dto/KisApiResult.java (신규)
@Getter
@RequiredArgsConstructor
public class KisApiResult<T> {
    private final T data;
    private final boolean hasNext;
}
```

### Research Insights — RestClient 패턴

**`.toEntity()` + `ParameterizedTypeReference` 조합:**
- Spring RestClient 공식 문서에서 지원 확인: `.toEntity(new ParameterizedTypeReference<KisApiResponse<T>>() {})` 정상 동작
- `ResponseEntity<T>` 반환으로 status, headers, body 모두 접근 가능
- Reference: [REST Clients :: Spring Framework](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html)

**`onStatus()` 에러 핸들링:**
- `.retrieve().onStatus(...).toEntity(...)` 체이닝으로 에러 핸들링과 헤더 접근을 동시에 처리
- 외부 API 에러 메시지를 사용자에게 직접 노출하지 않고, 범용 메시지로 래핑
- 기존 `SecApiClient`의 `onStatus()` 패턴 참고 (`stock/infrastructure/stock/sec/SecApiClient.java`)

**KIS 연속조회 프로토콜:**
- 요청 헤더 `tr_cont`: 최초 조회 `""`, 연속 조회 `"N"`
- 응답 헤더 `tr_cont`: `"F"` 또는 `"M"` = 다음 페이지 있음, `"D"` 또는 `"E"` = 마지막
- Reference: [KIS Developers API 문서](https://apiportal.koreainvestment.com/intro)

### KisApiResponse 구조 대응

KIS 뉴스 API의 응답 구조가 기존 `output` 필드와 다를 수 있다(`output1`, `output2` 등). `overseasnews` 도메인 전용 응답 DTO를 별도 생성하여 기존 `KisApiResponse`에 영향을 주지 않는다.

### Frontend — 포트폴리오 해외주식 [속보] 버튼 및 뉴스 패널

해외주식 카드에 [속보] 버튼을 추가하고, 클릭 시 인라인 패널이 펼쳐지며 "해외속보" / "해외뉴스종합" 탭으로 뉴스를 조회한다.

## Technical Considerations

### KIS API 종목코드 형식

- 해외속보 API(`FID_INPUT_ISCD`): 포트폴리오의 `stockCode` (예: AAPL) 직접 사용
- 해외뉴스종합 API(`SYMB`): 포트폴리오의 `stockCode` 직접 사용
- `NATION_CD`, `EXCHANGE_CD`: 포트폴리오의 `country`, `exchangeCode` 필드로 매핑
  - 매핑 테이블: `NAS`→`NAS`, `NYS`→`NYS`, `AMS`→`AMS`, `SHS`→`SHS`, `TSE`→`TSE`, `HKS`→`HKS`, `HNX`→`HNX`, `HSX`→`HSX`
  - `country` → `NATION_CD`: `US`→`US`, `CN`→`CN`, `JP`→`JP`, `HK`→`HK`, `VN`→`VN`
  - *Note: 실제 KIS API가 기대하는 값과 일치하는지 첫 구현 시 검증 필요*

### 페이지네이션 (해외뉴스종합)

- 요청 헤더 `tr_cont`: 최초 조회 `""` (빈 문자열), 연속 조회 `"N"`
- 응답 헤더 `tr_cont`: `"F"` 또는 `"M"` → 다음 페이지 있음, `"D"` 또는 `"E"` → 마지막 페이지
- 다음 페이지 요청 시 연속 키: 마지막 뉴스 항목의 `data_dt`, `data_tm` 값을 `DATA_DT`, `DATA_TM` 파라미터로 전달
- 첫 요청: `DATA_DT`, `DATA_TM` 빈 문자열
- 프론트엔드 응답 DTO에 `hasMore` boolean 포함하여 "더보기" 버튼 표시 제어

### 기존 시스템 영향

- 기존 `news` 도메인: 변경 없음
- 기존 `KisApiClient`: `getWithContinuation()` 메서드 1개 추가 (기존 `get()` 변경 없음)
- 기존 `KisApiResponse`: 변경 없음 (뉴스 전용 응답 DTO 별도 생성)

### 보안

#### 입력 검증 (Security Sentinel 권고 — Severity: HIGH)

컨트롤러에서 모든 사용자 입력을 검증한다:

| 파라미터 | 검증 방식 | 근거 |
|---|---|---|
| `exchangeCode` | `ExchangeCode` enum 타입으로 수신 (Spring이 자동 400 반환) | 기존 `KisStockPriceClient.getOverseasPrice()` 패턴과 일치 |
| `stockCode` | regex `^[A-Z0-9]{1,12}$` | 티커 심볼 형식 제한 |
| `country` | 허용 목록 검증 (`US`, `CN`, `JP`, `HK`, `VN`) | 알려진 국가코드만 허용 |
| `dataDt` | regex `^\d{8}$` 또는 빈 문자열 | 날짜 형식(YYYYMMDD) 제한 |
| `dataTm` | regex `^\d{6}$` 또는 빈 문자열 | 시간 형식(HHMMSS) 제한 |

SSRF 리스크는 LOW — `KisApiClient`가 base URL을 서버 설정에서 하드코딩하고, 사용자 파라미터는 `UriBuilder`로 쿼리 파라미터에만 주입되므로 host/path 변조 불가.

#### 에러 메시지 새니타이징 (Security Sentinel 권고 — Severity: MEDIUM)

현재 `GlobalExceptionHandler`가 `KisApiException.getMessage()`를 클라이언트에 그대로 반환하므로, KIS API 내부 에러 메시지가 노출될 수 있다.

```java
// OverseasNewsService에서 KisApiException을 catch하여 사용자 친화적 메시지로 변환
try {
    return kisOverseasNewsClient.getBreakingNews(stockCode, exchangeCode);
} catch (KisApiException e) {
    log.error("해외속보 조회 실패 [{}:{}]: {}", exchangeCode, stockCode, e.getMessage());
    throw new KisApiException("뉴스를 불러올 수 없습니다");  // 원본 메시지 제거
}
```

#### 인증/인가

- 인증: `ProdSecurityConfig`의 `.anyRequest().authenticated()` 규칙에 의해 JWT 인증이 자동 적용됨 (추가 작업 없음)
- 인가: 뉴스 데이터는 공개 시장 정보이므로 포트폴리오 소유권 검증은 불필요

### 성능

#### P0: KIS 전용 RestClient 타임아웃 설정

현재 KIS API 호출이 기본 RestClient를 사용하여 타임아웃이 설정되지 않음. `SecRestClientConfig` 패턴을 따라 전용 RestClient Bean을 생성한다.

```java
// overseasnews/infrastructure/kis/config/KisNewsRestClientConfig.java
// 또는 기존 KIS 모듈에 전용 RestClient 설정 추가
@Bean("kisRestClient")
public RestClient kisRestClient() {
    HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
    factory.setReadTimeout(Duration.ofSeconds(5));
    return RestClient.builder().requestFactory(factory).build();
}
```

*Note: 이 변경은 기존 KisApiClient의 RestClient Bean도 영향받으므로, 범위 확인 후 진행*

#### P1: 토큰 만료 단일 재시도

`KisTokenManager`의 캐시된 토큰이 `getAccessToken()` 확인 시점과 실제 API 호출 사이에 만료되면 첫 요청이 실패한다. `getWithContinuation()`에서 인증 오류 시 토큰 갱신 후 1회 재시도 로직을 추가한다.

#### 캐싱: 불필요 (의도적 미적용)

- 속보성 데이터이므로 최신 조회가 우선 ("속보성 데이터이므로 최신 조회 우선" — brainstorm key decision)
- 사용자 클릭 기반 온디맨드 호출로 요청 빈도가 낮음
- 캐시 키 카디널리티가 높아 (stockCode × newsType × pagination) 적중률이 낮음
- 개인 포트폴리오 앱으로 KIS API Rate Limit(20건/초)에 도달할 가능성 없음

### 프론트엔드 레이스 컨디션 방지

#### Generation Counter 패턴 적용 (Frontend Races 리뷰 — CRITICAL)

기존 `ecos.js`, `financial.js`에서 이미 사용 중인 generation counter 패턴을 해외뉴스에도 적용한다. 이 패턴은 다음 레이스 컨디션을 모두 방지한다:

| 시나리오 | 문제 | 방지 방법 |
|---|---|---|
| 종목 전환 중 로딩 | Stock A 응답이 Stock B 패널에 표시됨 | generation 증가 → 이전 응답 무시 |
| 빠른 탭 전환 | 느린 이전 탭 응답이 현재 탭 데이터를 덮어씀 | generation 증가 → 이전 응답 무시 |
| 더보기 중 탭 전환 | 이전 탭 page 2 데이터가 새 탭에 append됨 | 동일 generation counter 공유 |
| 패널 닫기 중 로딩 | 닫힌 패널의 응답이 상태를 오염시킴 | 닫기 시 generation 증가 |

```javascript
// portfolio.js 상태 추가
_overseasNewsGeneration: 0,
_overseasNewsDebounceTimer: null,
overseasNews: {
    activeTab: 'breaking',        // 즉시 전환 (visual)
    selectedItemId: null,
    breaking: { list: [], loading: false, error: null },
    comprehensive: { list: [], loading: false, error: null, hasMore: false, lastDt: '', lastTm: '' }
},

// 탭 전환: 시각적 즉시 전환 + API 호출 debounce
switchOverseasNewsTab(tab) {
    this.portfolio.overseasNews.activeTab = tab;  // 즉시 시각 전환
    this.portfolio.overseasNews[tab].list = [];    // 이전 데이터 즉시 제거
    clearTimeout(this.portfolio._overseasNewsDebounceTimer);
    this.portfolio._overseasNewsDebounceTimer = setTimeout(() => {
        this.loadOverseasNews(tab);
    }, 200);  // 200ms debounce (300ms 대비 더 반응적)
},

// API 호출: generation counter로 레이스 컨디션 방지
async loadOverseasNews(tab) {
    const gen = ++this.portfolio._overseasNewsGeneration;
    const tabState = this.portfolio.overseasNews[tab];
    tabState.loading = true;
    tabState.error = null;
    try {
        const result = tab === 'breaking'
            ? await API.getOverseasBreakingNews(stockCode, exchangeCode)
            : await API.getOverseasComprehensiveNews(stockCode, exchangeCode, country);
        if (gen !== this.portfolio._overseasNewsGeneration) return;  // stale 응답 무시
        tabState.list = result.items;
        if (tab === 'comprehensive') {
            tabState.hasMore = result.hasMore;
            tabState.lastDt = result.lastDt;
            tabState.lastTm = result.lastTm;
        }
    } catch (e) {
        if (gen !== this.portfolio._overseasNewsGeneration) return;
        tabState.error = '뉴스를 불러올 수 없습니다';
    } finally {
        if (gen === this.portfolio._overseasNewsGeneration) tabState.loading = false;
    }
}
```

### UI 동작 규칙

- [속보] 버튼은 해외주식(`item.stockDetail?.country !== 'KR'`)에만 표시
- 동일 종목 재클릭 시 패널 토글 (닫힘) + generation 증가 + debounce timer 해제
- 다른 종목 [속보] 클릭 시 이전 패널 닫고 새 패널 열림
- 속보 패널과 기존 키워드 뉴스 패널은 동시에 열리지 않음 (하나를 열면 다른 하나는 닫힘)
- 탭 전환 시 시각적 상태 즉시 전환, API 호출은 200ms debounce
- "더보기" 클릭도 동일 generation counter 사용

### 크로스 도메인 의존성 (Architecture Strategist 권고)

`KisOverseasNewsClient`가 `stock/infrastructure/stock/kis/KisApiClient`를 사용하면 도메인 간 수평 의존성이 발생한다. 이 문제의 근본 해결책은 KIS 공유 인프라(`KisApiClient`, `KisTokenManager`, `KisProperties`)를 `infrastructure/kis/`로 추출하는 것이다 (기존 `infrastructure/security/` 선례).

**이번 범위**: 공유 인프라 추출은 별도 리팩토링 이슈로 분리한다. 이번 구현에서는 `overseasnews`가 `stock.infrastructure.stock.kis.KisApiClient`를 직접 참조하되, 의도적 기술 부채로 문서화한다.

## Acceptance Criteria

- [ ] 포트폴리오 해외주식 종목에 [속보] 버튼이 표시된다
- [ ] [속보] 클릭 시 뉴스 패널이 열리고 "해외속보" / "해외뉴스종합" 두 탭이 표시된다
- [ ] 해외속보 탭: KIS API 호출 후 작성일시, 제목, 자료원이 표시된다 (최대 100건)
- [ ] 해외뉴스종합 탭: KIS API 호출 후 조회일시, 제목, 중분류명, 자료원, 종목명이 표시된다
- [ ] 해외뉴스종합 탭: "더보기" 버튼으로 다음 페이지 로드가 가능하다
- [ ] 기존 키워드 뉴스 시스템에 영향이 없다
- [ ] 로딩/에러/빈 상태가 적절히 표시된다
- [ ] DB 저장 없이 온디맨드 API 호출로만 동작한다
- [ ] 컨트롤러 입력 검증: exchangeCode(enum), stockCode(regex), country(whitelist), dataDt/dataTm(format)
- [ ] KIS API 에러 메시지가 클라이언트에 직접 노출되지 않는다
- [ ] 빠른 탭 전환/종목 전환 시 레이스 컨디션이 발생하지 않는다 (generation counter)

## Dependencies & Risks

| 항목 | 리스크 | 대응 |
|---|---|---|
| KIS API 응답 구조 | `output` vs `output1` 등 실제 필드명 불확실 | 첫 구현 시 실제 API 호출로 검증, 전용 DTO로 격리 |
| KIS API Rate Limit | 빠른 탭 전환/더보기 클릭 시 제한 초과 가능 | 프론트엔드 debounce 200ms + generation counter로 불필요한 요청 방지 |
| 거래소/국가 코드 매핑 | KIS API가 기대하는 코드와 불일치 가능 | 첫 구현 시 주요 거래소(NAS, NYS) 우선 검증 |
| `KisApiClient` 확장 | 공유 인프라 변경이므로 기존 기능에 영향 가능 | 기존 `get()` 변경 없이 `getWithContinuation()` 추가만 수행 |
| 크로스 도메인 의존성 | `overseasnews` → `stock` 인프라 참조 | 의도적 기술 부채로 문서화, 별도 리팩토링 이슈 분리 |
| KIS RestClient 타임아웃 미설정 | 응답 없을 시 스레드 무한 대기 | 전용 RestClient Bean 생성 (connect 3s / read 5s) |
| 토큰 만료 타이밍 | 캐시 확인↔API 호출 사이 만료 시 첫 요청 실패 | 인증 오류 시 토큰 갱신 후 1회 재시도 |

## 작업 리스트

### Phase 1: 백엔드 인프라 확장

- [x] `KisApiResult<T>` 래퍼 클래스 생성 (`stock/infrastructure/stock/kis/dto/KisApiResult.java`) — `data`, `hasNext` 필드
- [x] `KisApiClient.getWithContinuation()` 메서드 추가 — `.toEntity()` + `onStatus()` 패턴, `tr_cont` 요청/응답 헤더 처리
- [ ] KIS 전용 RestClient 타임아웃 설정 확인/추가 (connect 3s, read 5s)

### Phase 2: `overseasnews` 도메인 — 백엔드

- [x] KIS 뉴스 API 응답 DTO 생성: `KisBreakingNewsOutput`, `KisNewsOutput` (`overseasnews/infrastructure/kis/dto/`) — `@Getter @NoArgsConstructor @JsonProperty` 패턴
- [ ] (조건부) KIS 뉴스 전용 응답 래퍼 `KisNewsApiResponse` 생성 — KIS API 응답 구조가 `output`이 아닌 경우에만
- [x] KIS 뉴스 클라이언트 생성: `KisOverseasNewsClient` (`overseasnews/infrastructure/kis/`)
- [x] 애플리케이션 서비스 생성: `OverseasNewsService` (`overseasnews/application/`) — 인라인 매핑 + 에러 메시지 새니타이징
- [x] 응답 DTO 생성: `BreakingNewsResponse`, `ComprehensiveNewsResponse` (`overseasnews/application/dto/`)
- [x] 컨트롤러 생성: `OverseasNewsController` (`overseasnews/presentation/`) — 입력 검증 포함
  - `GET /api/overseas-news/breaking?stockCode={}&exchangeCode={}` — 해외속보
  - `GET /api/overseas-news/comprehensive?stockCode={}&exchangeCode={}&country={}&dataDt={}&dataTm={}` — 해외뉴스종합

### Phase 3: 프론트엔드

- [x] `portfolio.js` 상태 추가: `overseasNews` 객체, `_overseasNewsGeneration`, `_overseasNewsDebounceTimer`
- [x] `api.js`에 해외뉴스 API 메서드 추가: `getOverseasBreakingNews()`, `getOverseasComprehensiveNews()`
- [x] `portfolio.js`에 뉴스 로드/탭 전환/더보기 메서드 추가 — generation counter 패턴 적용
- [x] `index.html` 해외주식 섹션에 [속보] 버튼 추가 (line 982 부근, `getOverseasStocks()` 루프 내 버튼 영역)
- [x] `index.html`에 속보 뉴스 패널 UI 추가:
  - 해외속보/해외뉴스종합 탭 헤더
  - 뉴스 리스트 (기존 키워드 뉴스 UI 스타일 참고: `text-sm text-gray-800 font-medium`, `border-b border-gray-100`)
  - 로딩/에러/빈 상태 표시
  - "더보기" 버튼 (해외뉴스종합 탭, `hasMore` 조건)
  - 반응형 대응 (모바일에서도 패널 정상 표시 — `docs/solutions/ui-bugs/responsive-design-tailwind-alpine.md` 참고)

### Phase 4: 검증

- [ ] NAS/NYS 종목(예: AAPL)으로 해외속보 API 응답 구조 검증 (`output` 필드명, 각 필드 실제 값 확인)
- [ ] NAS/NYS 종목으로 해외뉴스종합 API 응답 구조 및 페이지네이션 검증 (`tr_cont` 헤더 동작 확인)
- [ ] 레이스 컨디션 테스트: Chrome DevTools Network throttling (Slow 3G) 상태에서 빠른 탭/종목 전환
- [ ] 전체 흐름 E2E 동작 확인

## Sources & References

- **Origin brainstorm:** [docs/brainstorms/2026-04-03-kis-overseas-news-requirements.md](docs/brainstorms/2026-04-03-kis-overseas-news-requirements.md) — Key decisions: 기존 뉴스 시스템과 분리, 실시간 API 호출, 포트폴리오 화면 내 배치
- **KisApiClient**: `src/main/java/.../stock/infrastructure/stock/kis/KisApiClient.java:38` — 현재 `get()` 메서드, `getWithContinuation()` 추가 대상
- **KisApiResponse**: `src/main/java/.../stock/infrastructure/stock/kis/dto/KisApiResponse.java:13` — 현재 `output` 필드만 존재
- **KisStockPriceClient**: `src/main/java/.../stock/infrastructure/stock/kis/KisStockPriceClient.java:97` — 기존 해외 현재가 조회 패턴 참고 (ExchangeCode enum 사용)
- **SecRestClientConfig**: `src/main/java/.../stock/infrastructure/stock/sec/config/SecRestClientConfig.java` — RestClient 타임아웃 설정 참고 패턴
- **SecApiClient**: `src/main/java/.../stock/infrastructure/stock/sec/SecApiClient.java` — `onStatus()` 에러 핸들링 참고 패턴
- **Chatbot port 패턴**: `src/main/java/.../chatbot/application/port/LlmPort.java` — domain 레이어 없는 pass-through 선례
- **포트폴리오 해외주식 UI**: `src/main/resources/static/index.html:982` — 해외주식 루프 시작점
- **포트폴리오 JS**: `src/main/resources/static/js/components/portfolio.js:279` — `getOverseasStocks()` 메서드
- **Generation counter 선례**: `src/main/resources/static/js/components/ecos.js` — `_ecosRequestGeneration` 패턴
- **기존 뉴스 패널 UI**: `src/main/resources/static/index.html:939-975` — 키워드 뉴스 패널 구조 참고
- **반응형 학습**: `docs/solutions/ui-bugs/responsive-design-tailwind-alpine.md` — 모바일 패널 대응 참고
- **API 클라이언트**: `src/main/resources/static/js/api.js` — 프론트엔드 API 호출 패턴

### External References

- [REST Clients :: Spring Framework](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html) — `toEntity()`, `onStatus()` 공식 문서
- [KIS Developers](https://apiportal.koreainvestment.com/intro) — KIS Open API 연속조회 프로토콜
- [Alpine.js Async Patterns](https://alpinejs.dev/advanced/async) — async 함수 지원 가이드