# Chatbot 설계

## 작업 리스트

- [x] `LlmPort` 인터페이스 생성 (application/port)
- [x] `GeminiProperties` 설정 클래스 생성 (infrastructure)
- [x] `GeminiConfig` WebClient Bean 등록 (infrastructure)
- [x] `GeminiRequest` / `GeminiStreamChunk` DTO 생성 (infrastructure)
- [x] `GeminiAdapter` 구현 (infrastructure)
- [x] `ChatMode` enum 생성 (application/dto)
- [x] `ChatRequest` DTO 생성 (application/dto)
- [x] `ChatContextBuilder` 구현 (application)
- [x] `ChatService` 구현 (application)
- [x] `ChatController` 구현 (presentation)
- [x] `application.yml`에 Gemini API 키 설정 추가

## 배경

Gemini API(무료 티어, `gemini-1.5-flash`)를 사용하는 챗봇 서비스 추가.
두 가지 **모드**를 분리하여 지원한다:
1. **PORTFOLIO 모드 (포트폴리오 비중 분석)**: 현재 자산 배분이 올바른지, 변경이 필요한지 질문
2. **FINANCIAL 모드 (종목 재무 분석)**: UI에서 종목을 선택하면 서버가 DART 재무지표를 자동 조회하여 매수/매도/보유 의견 제공

## 핵심 결정

- **스트리밍**: `Flux<String>` + SSE(`text/event-stream`) — WebFlux 기반
- **LlmPort 위치**: `chatbot/application/port/` — `Flux<String>` 반환으로 Reactor 의존이 불가피, domain 계층 규칙("순수 Java 표준 라이브러리만") 위반을 피해 application/port에 위치
- **챗 모드 분리**: `ChatMode` enum으로 PORTFOLIO / FINANCIAL 모드 분리 — 각 모드에 맞는 컨텍스트만 제공
- **재무 데이터 조회 방식**: 서버가 기존 `StockFinancialService`를 DI로 직접 호출하여 수익성(M210000) + 안정성(M220000) 지표 자동 조회 — 클라이언트는 `stockCode`만 전달
- **종목 범위**: 보유 종목 + 미보유 종목 모두 분석 가능 — UI에서 드롭다운/검색으로 종목코드 선택
- **보유 종목 재무 분석 시**: 포트폴리오 정보(매수 평균가, 수량, 투자금액)도 컨텍스트에 포함
- **조회 기간**: 최신 사업보고서(연간, reportCode=11011) 기준
- **컨텍스트 구성**:
  - PORTFOLIO 모드: 포트폴리오 비중(`PortfolioAllocationService`) + 보유 종목 목록(`PortfolioService`)
  - FINANCIAL 모드: 재무지표(`StockFinancialService`) + 보유 종목이면 포트폴리오 정보 추가
- **Gemini 모델**: `gemini-1.5-flash` (무료 티어, 분당 15회, 일 1500회)
- **API 키 관리**: `application-global.properties`에 `gemini.api-key` 환경변수로 관리

## 패키지 구조

```
chatbot/
├── application/
│   ├── port/
│   │   └── LlmPort.java                   ← Flux<String> stream(String systemPrompt, String userMessage)
│   ├── dto/
│   │   ├── ChatMode.java                  ← enum: PORTFOLIO, FINANCIAL
│   │   └── ChatRequest.java               ← userId, message, chatMode, stockCode(nullable)
│   ├── ChatContextBuilder.java            ← 모드별 컨텍스트 조합
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

### ChatMode
위치: `chatbot/application/dto/ChatMode.java`

- `PORTFOLIO`: 포트폴리오 비중 분석 모드
- `FINANCIAL`: 종목 재무 분석 모드

### ChatRequest
위치: `chatbot/application/dto/ChatRequest.java`

- `userId`: Long
- `message`: String
- `chatMode`: ChatMode (PORTFOLIO / FINANCIAL)
- `stockCode`: String (nullable — FINANCIAL 모드에서만 사용, UI에서 종목 선택 시 전달)

### ChatContextBuilder
위치: `chatbot/application/ChatContextBuilder.java`

모드별 분기 로직:
- **PORTFOLIO 모드**:
  - `PortfolioAllocationService.getAllocation(userId)`로 자산 유형별 비중(%) 조회
  - `PortfolioService.getItems(userId)`로 보유 종목 목록 조회
- **FINANCIAL 모드**:
  - `StockFinancialService.getFinancialIndices(stockCode, year, "11011", "M210000")`로 수익성 지표 조회
  - `StockFinancialService.getFinancialIndices(stockCode, year, "11011", "M220000")`로 안정성 지표 조회
  - 보유 종목인 경우: `PortfolioService.getItems(userId)`에서 해당 종목 매칭 → 매수 평균가, 수량, 투자금액 포함
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
- request body에 `message`, `chatMode`, `stockCode`(nullable) 포함
- `Flux<String>` 반환 → SSE 스트리밍

[예시 코드](./examples/presentation-example.md)

## 주의사항

- Gemini 무료 티어 한도: 분당 15회, 일 1500회 — 초과 시 `429` 에러 발생, 별도 처리 불필요 (그대로 예외 전파)
- `ProdSecurityConfig`는 `.anyRequest().authenticated()`로 `/api/chat`도 자동 인증 적용, 별도 설정 불필요
- `GeminiAdapter`는 `ObjectMapper`를 주입받아 SSE `data` 필드를 `GeminiStreamChunk`로 역직렬화
- `WebFlux`와 `WebMVC`를 함께 사용 중이므로 `WebClient`는 기존 `RestClientConfig` 패턴 참고
- FINANCIAL 모드에서 `year`는 현재 연도 기준 최신 사업보고서를 조회 (보고서 미공시 시 전년도 fallback)