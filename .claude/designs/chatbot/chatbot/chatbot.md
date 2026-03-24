# Chatbot 설계

## 작업 리스트

- [ ] `LlmPort` 인터페이스 생성 (application/port)
- [ ] `GeminiProperties` 설정 클래스 생성 (infrastructure)
- [ ] `GeminiConfig` WebClient Bean 등록 (infrastructure)
- [ ] `GeminiRequest` / `GeminiStreamChunk` DTO 생성 (infrastructure)
- [ ] `GeminiAdapter` 구현 (infrastructure)
- [ ] `ChatRequest` / `FinancialData` DTO 생성 (application)
- [ ] `ChatContextBuilder` 구현 (application)
- [ ] `ChatService` 구현 (application)
- [ ] `ChatController` 구현 (presentation)
- [ ] `application-global.properties`에 Gemini API 키 설정 추가

## 배경

Gemini API(무료 티어, `gemini-1.5-flash`)를 사용하는 챗봇 서비스 추가.
두 가지 사용 시나리오를 지원한다:
1. **포트폴리오 비중 분석**: 현재 자산 배분이 올바른지, 변경이 필요한지 질문
2. **종목 재무 분석**: 특정 종목의 재무지표 값(PER, PBR, EPS 등)을 직접 전달하고, 현재 주가 대비 실적 평가 질문

## 핵심 결정

- **스트리밍**: `Flux<String>` + SSE(`text/event-stream`) — WebFlux 기반
- **LlmPort 위치**: `chatbot/application/port/` — `Flux<String>` 반환으로 Reactor 의존이 불가피, domain 계층 규칙("순수 Java 표준 라이브러리만") 위반을 피해 application/port에 위치
- **재무 데이터 전달 방식**: 클라이언트가 요청 body에 재무지표를 직접 포함 — 서버가 외부 재무 API를 연동하지 않음 (연동 범위 외)
- **컨텍스트**: 포트폴리오 비중(`PortfolioAllocationService`) + 보유 종목 목록(`PortfolioService`) + 요청에 포함된 재무 데이터
- **Gemini 모델**: `gemini-1.5-flash` (무료 티어, 분당 15회, 일 1500회)
- **API 키 관리**: `application-global.properties`에 `gemini.api-key` 환경변수로 관리

## 패키지 구조

```
chatbot/
├── application/
│   ├── port/
│   │   └── LlmPort.java                   ← Flux<String> stream(String systemPrompt, String userMessage)
│   ├── dto/
│   │   ├── ChatRequest.java               ← userId, message, financialData(optional)
│   │   └── FinancialData.java             ← ticker, currentPrice, per, pbr, eps, roe 등
│   ├── ChatContextBuilder.java            ← 포트폴리오 비중 + 보유 종목 + 재무 데이터 조합
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
- Gemini `alt=sse` 응답은 `text/event-stream` 형식(`data: {...JSON...}`)이므로 `Accept: text/event-stream` 헤더 필수
- `bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})` → `ServerSentEvent::data` 추출 → `ObjectMapper`로 `GeminiStreamChunk` 역직렬화
- chunk에서 텍스트 추출 → `Flux<String>` 반환

[예시 코드](./examples/infrastructure-adapter-example.md)

### ChatRequest / FinancialData
위치: `chatbot/application/dto/`

**ChatRequest**
- `userId`: Long
- `message`: String
- `financialData`: `FinancialData` (nullable — 재무 분석 질문 시에만 포함)

**FinancialData**
- `ticker`: String (종목명 또는 코드)
- `currentPrice`: BigDecimal (현재 주가)
- `per`: BigDecimal (nullable)
- `pbr`: BigDecimal (nullable)
- `eps`: BigDecimal (nullable)
- `roe`: BigDecimal (nullable)
- `memo`: String (nullable — 추가 설명)

### ChatContextBuilder
위치: `chatbot/application/ChatContextBuilder.java`

- `PortfolioAllocationService.getAllocation(userId)`로 자산 유형별 비중(%) 조회
- `PortfolioService.getItems(userId)`로 보유 종목 목록 조회
- `request.financialData()`가 있으면 재무지표 섹션 추가
- 조합한 컨텍스트를 시스템 프롬프트 문자열로 반환

### ChatService
위치: `chatbot/application/ChatService.java`

- `ChatContextBuilder`로 시스템 프롬프트 구성
- `LlmPort.stream(systemPrompt, request.message())` 호출
- `Flux<String>` 그대로 반환

[예시 코드](./examples/application-example.md)

### ChatController
위치: `chatbot/presentation/ChatController.java`

- `POST /api/chat?userId={userId}`
- `produces = MediaType.TEXT_EVENT_STREAM_VALUE`
- 기존 프로젝트 패턴에 따라 `@RequestParam Long userId`로 수신
- `Flux<String>` 반환 → SSE 스트리밍

[예시 코드](./examples/presentation-example.md)

## 주의사항

- Gemini 무료 티어 한도: 분당 15회, 일 1500회 — 초과 시 `429` 에러 발생, 별도 처리 불필요 (그대로 예외 전파)
- `ProdSecurityConfig`는 `.anyRequest().authenticated()`로 `/api/chat`도 자동 인증 적용, 별도 설정 불필요
- `GeminiAdapter`는 `ObjectMapper`를 주입받아 SSE `data` 필드를 `GeminiStreamChunk`로 역직렬화
- `WebFlux`와 `WebMVC`를 함께 사용 중이므로 `WebClient`는 기존 `RestClientConfig` 패턴 참고
