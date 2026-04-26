# ARCHITECTURE.md

stock-market 프로젝트의 패키지 구조, 계층 규칙, 의존성 방향 기준 문서입니다.

---

## 1. 아키텍처 개요

- **Layered Architecture + DDD** 기반, 도메인별 패키지 분리
- 의존성 방향: `presentation → application → domain ← infrastructure`
- 아키텍처 변경은 명시적 요청 없이 금지

---

## 2. 도메인

| 도메인 | 설명 |
|---|---|
| **User** | 회원 정보, Kakao OIDC 소셜 로그인 |
| **News** | 키워드 기반 뉴스 배치 수집, Region별(국내/해외) 검색 포트 분기 |
| **Economics** | 한국은행 ECOS API 연동, 100대 경제 지표 조회/저장, 배치+캐시 Warmup |
| **Notification** | 이메일 알림 (인터페이스 + 구현체) |
| **Chatbot** | Gemini API 기반 LLM 챗봇 (포트폴리오/뉴스/경제지표 컨텍스트 주입, SSE 스트리밍) |

---

## 3. 패키지 구조

```
src/main/java/com/thlee/stock/market/stockmarket/
│
├── config/                          # 글로벌 설정
│
├── infrastructure/                  # 공통 인프라 (도메인 독립)
│   └── security/
│       ├── config/                  # Spring Security 설정 (Dev/Prod 분리)
│       └── jwt/                     # JWT 토큰 구현체 + 예외
│
├── user/
│   ├── domain/
│   │   ├── model/                   # User, OAuthAccount, Value Objects
│   │   ├── oauth/                   # OIDC 파싱 인터페이스
│   │   ├── repository/              # 포트 인터페이스
│   │   ├── service/                 # JwtTokenProvider(포트), OAuthConnectionService
│   │   └── exception/
│   ├── application/
│   │   ├── AuthService, OAuthLoginService, KakaoOAuthService
│   │   └── dto/
│   ├── infrastructure/
│   │   ├── persistence/             # Entity, Repository 구현체, Mapper
│   │   └── oauth/kakao/             # Kakao OAuth 클라이언트, JWKS, DTO, 예외
│   └── presentation/                # AuthController
│
├── news/
│   ├── domain/
│   │   ├── model/                   # News, Keyword, Region, NewsPurpose, NewsSearchResult
│   │   ├── repository/              # 포트 인터페이스
│   │   └── service/                 # NewsSearchPort(포트), NewsSearchPortFactory(포트)
│   ├── application/
│   │   ├── NewsQueryService, NewsSearchService, NewsSaveService
│   │   ├── KeywordService / KeywordNewsBatchService (인터페이스 + Impl)
│   │   ├── dto/
│   │   └── vo/                      # KeywordSearchContext
│   ├── infrastructure/
│   │   ├── config/                  # RestClientConfig
│   │   ├── NewsSearchPortFactoryImpl # Region 기반 Factory 구현체
│   │   ├── persistence/             # Entity, Repository 구현체, Mapper
│   │   ├── scheduler/               # KeywordNewsBatchScheduler
│   │   └── infrastructure/          # 외부 뉴스 API 어댑터
│   │       ├── common/              # HtmlTextCleaner, NewsApiException
│   │       ├── naver/               # 네이버 뉴스 API (국내)
│   │       ├── gnews/               # GNews API (해외)
│   │       └── newsapi/             # NewsAPI (해외)
│   └── presentation/                # KeywordController
│
├── economics/
│   ├── domain/
│   │   ├── model/                   # EcosIndicator, EcosIndicatorLatest, Category 등
│   │   ├── repository/              # 포트 인터페이스
│   │   └── service/                 # EcosIndicatorPort (포트)
│   ├── application/
│   │   ├── EcosIndicatorService     # 조회 유스케이스
│   │   └── EcosIndicatorSaveService # 저장 유스케이스
│   ├── infrastructure/
│   │   ├── korea/ecos/              # ECOS API 어댑터 (Client, Adapter, Config, DTO, Exception)
│   │   ├── scheduler/               # 배치 스케줄러, WarmupListener
│   │   └── persistence/             # Entity, Repository 구현체, Mapper
│   └── presentation/
│       ├── EcosIndicatorController
│       └── dto/                     # IndicatorResponse, CategoryResponse
│
├── notification/
│   ├── domain/                      # NotificationRequest, NotificationService (인터페이스)
│   └── infrastructure/email/        # EmailNotificationService (구현체)
│
├── chatbot/
│   ├── application/
│   │   ├── port/                    # LlmPort (Flux<String>, Reactor 의존으로 application/port에 위치)
│   │   ├── dto/                     # ChatRequest
│   │   ├── ChatContextBuilder       # 포트폴리오/뉴스/경제지표 컨텍스트 조합
│   │   └── ChatService              # 유스케이스 진입점
│   ├── infrastructure/
│   │   └── gemini/                  # GeminiAdapter(LlmPort 구현), WebClient, DTO, Config
│   └── presentation/                # ChatController (POST /api/chat, SSE 스트리밍)
│
└── stocknote/
    ├── domain/
    │   ├── model/                   # StockNote, StockNoteTag, StockNotePriceSnapshot, StockNoteVerification, StockNoteCustomTag + enums
    │   └── repository/              # 포트 인터페이스 5개
    ├── application/
    │   ├── StockNoteWriteService / ReadService / VerificationService
    │   ├── StockNoteSnapshotService + StockNoteSnapshotAsyncDispatcher (event AFTER_COMMIT + @Async)
    │   ├── StockNoteDashboardService (Caffeine TTL 30m), PatternMatchService, ChartService, CustomTagService
    │   ├── dto/                     # *Result / *Command (유스케이스 입출력)
    │   └── event/, exception/
    ├── infrastructure/
    │   ├── persistence/             # Entity 5종, JpaRepository, RepositoryImpl, Mapper
    │   ├── scheduler/               # StockNoteSnapshotScheduler (국내 16:00/해외 07:00 KST), BusinessDayCalculator
    │   ├── async/                   # 전용 ThreadPoolTaskExecutor + MdcTaskDecorator
    │   └── cache/                   # StocknoteCacheConfig
    └── presentation/                # StockNoteController, VerificationController, AnalyticsController, CustomTagController, ExceptionHandler
```

---

## 4. 레이어 규칙

### presentation
- HTTP 요청/응답 처리, DTO 변환
- **금지**: 비즈니스 로직, domain 모델 직접 노출, @Transactional

### application
- 유스케이스 구현, @Transactional 경계
- 여러 도메인 조합, domain 서비스 호출
- **금지**: 직접 외부 API 호출, domain 규칙 중복

### domain
- 순수 비즈니스 규칙, Java 표준 라이브러리만 사용
- repository/service는 **인터페이스(포트)만** 정의
- **금지**: Spring/JPA/외부 시스템 의존

### infrastructure
- DB, 외부 API, 인프라 연동 구현 (어댑터)
- domain.repository 인터페이스 구현
- Entity ↔ Domain Model 변환은 `mapper/` 패키지에서 담당

---

## 5. 핵심 설계 원칙

| 원칙 | 설명 |
|---|---|
| **Entity 연관관계 금지** | JPA Entity는 ID 기반 참조만 사용 |
| **도메인 간 참조** | 다른 도메인의 식별자(ID/Code)만 참조, 조합은 application 계층에서 |
| **포트-어댑터 패턴** | domain에 인터페이스(포트), infrastructure에 구현체(어댑터) |
| **@Transactional** | application 계층에서만 사용 |
| **DTO 변환 흐름** | `Request DTO → Application DTO → Domain Model → Entity` |
| **DTO/Entity 경계** | Entity는 API 노출 금지, Domain Model은 JSON 직렬화 금지 |
| **예외 처리** | domain.exception으로 비즈니스 규칙 위반 표현, presentation에서 HTTP Status 매핑 |

---