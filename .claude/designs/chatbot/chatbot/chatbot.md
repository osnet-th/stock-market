# Chatbot 설계

## 작업 리스트

- [ ] `LlmPort` 인터페이스 생성 (application/port)
- [ ] `GeminiProperties` 설정 클래스 생성 (infrastructure)
- [ ] `GeminiConfig` WebClient Bean 등록 (infrastructure)
- [ ] `GeminiRequest` / `GeminiStreamChunk` DTO 생성 (infrastructure)
- [ ] `GeminiAdapter` 구현 (infrastructure)
- [ ] `ChatRequest` DTO 생성 (application)
- [ ] `ChatContextBuilder` 구현 (application)
- [ ] `ChatService` 구현 (application)
- [ ] `ChatController` 구현 (presentation)
- [ ] `application-global.properties`에 Gemini API 키 설정 추가
- [ ] Security Config에서 `/api/chat/**` 인증 설정 추가

## 배경

Gemini API(무료 티어, `gemini-1.5-flash`)를 사용하여 포트폴리오/뉴스/경제지표 데이터를 컨텍스트로 활용하는 스트리밍 챗봇 서비스를 추가한다.

## 핵심 결정

- **스트리밍**: `Flux<String>` + SSE(`text/event-stream`) — WebFlux 기반
- **컨텍스트 주입 방식**: ChatService가 기존 서비스(Portfolio, News, Ecos)에서 데이터를 조합해 시스템 프롬프트에 주입 (DB 저장 없음, 메모리 처리)
- **LlmPort 위치**: `chatbot/application/port/` — `Flux<String>` 반환으로 Reactor 의존이 불가피, domain 계층 규칙("순수 Java 표준 라이브러리만") 위반을 피해 application/port에 위치
- **Gemini 모델**: `gemini-1.5-flash` (무료 티어, 분당 15회, 일 1500회)
- **API 키 관리**: `application-global.properties`에 `gemini.api-key` 환경변수로 관리

## 패키지 구조

```
chatbot/
├── application/
│   ├── port/
│   │   └── LlmPort.java                   ← Flux<String> stream(String systemPrompt, String userMessage)
│   ├── dto/
│   │   └── ChatRequest.java               ← userId, message
│   ├── ChatContextBuilder.java            ← 포트폴리오 + 뉴스 + 경제지표 컨텍스트 조합
│   └── ChatService.java                   ← 유스케이스 진입점
├── infrastructure/
│   └── gemini/
│       ├── config/
│       │   ├── GeminiProperties.java
│       │   └── GeminiConfig.java          ← WebClient Bean
│       ├── dto/
│       │   ├── GeminiRequest.java
│       │   └── GeminiStreamChunk.java
│       └── GeminiAdapter.java             ← LlmPort 구현체
└── presentation/
    └── ChatController.java                ← POST /api/chat
```

## 구현

### LlmPort
위치: `chatbot/application/port/LlmPort.java`

[예시 코드](./examples/domain-example.md)

### GeminiProperties / GeminiConfig
위치: `chatbot/infrastructure/gemini/config/`

- `GeminiProperties`: `@ConfigurationProperties(prefix = "gemini")` — `apiKey`, `model`(기본값 `gemini-1.5-flash`), `baseUrl`
- `GeminiConfig`: `WebClient` Bean — `baseUrl` 설정, `Content-Type: application/json`

[예시 코드](./examples/infrastructure-config-example.md)

### GeminiRequest / GeminiStreamChunk
위치: `chatbot/infrastructure/gemini/dto/`

- `GeminiRequest`: Gemini API 요청 구조 (`systemInstruction`, `contents`, `generationConfig`)
- `GeminiStreamChunk`: SSE 응답 파싱용 (`candidates[0].content.parts[0].text`)

[예시 코드](./examples/infrastructure-dto-example.md)

### GeminiAdapter
위치: `chatbot/infrastructure/gemini/GeminiAdapter.java`

- `LlmPort` 구현
- `WebClient`로 `POST /v1beta/models/{model}:streamGenerateContent?key={apiKey}&alt=sse` 호출
- `bodyToFlux(GeminiStreamChunk.class)`로 스트리밍 수신
- chunk에서 텍스트 추출 → `Flux<String>` 반환

[예시 코드](./examples/infrastructure-adapter-example.md)

### ChatRequest
위치: `chatbot/application/dto/ChatRequest.java`

- `userId`: Long
- `message`: String

### ChatContextBuilder
위치: `chatbot/application/ChatContextBuilder.java`

- `PortfolioService.getItems(userId)`로 보유 자산 조회 (`getItemName()`, `getAssetType()`, `getRegion()` 사용)
- `KeywordService.getActiveKeywordsByUser(userId)`로 활성 키워드 조회 → `NewsQueryService.getNewsByKeywordId(keywordId, 0, 3)`로 DB 저장 뉴스 조회 (외부 API 호출 없음)
- `EcosIndicatorService.getIndicatorsByCategory(INTEREST_RATE)` 등 주요 경제지표 조회 (`keystatName()`, `dataValue()` 사용)
- 조합한 컨텍스트를 시스템 프롬프트 문자열로 반환

### ChatService
위치: `chatbot/application/ChatService.java`

- `ChatContextBuilder`로 시스템 프롬프트 구성
- `LlmPort.stream(systemPrompt, request.message())` 호출
- `Flux<String>` 그대로 반환

[예시 코드](./examples/application-example.md)

### ChatController
위치: `chatbot/presentation/ChatController.java`

- `POST /api/chat`
- `produces = MediaType.TEXT_EVENT_STREAM_VALUE`
- `@AuthenticationPrincipal`로 userId 추출
- `Flux<String>` 반환 → SSE 스트리밍

[예시 코드](./examples/presentation-example.md)

## 주의사항

- Gemini 무료 티어 한도: 분당 15회, 일 1500회 — 초과 시 `429` 에러 발생, 별도 처리 불필요 (그대로 예외 전파)
- `ChatContextBuilder`에서 외부 뉴스 API 호출 시 타임아웃(연결 3초, 읽기 5초) 기존 설정 그대로 적용
- SSE 응답이므로 Security Config에서 CSRF 예외 처리 확인 필요
- `WebFlux`와 `WebMVC`를 함께 사용 중이므로 `WebClient`는 기존 `RestClientConfig` 패턴 참고
