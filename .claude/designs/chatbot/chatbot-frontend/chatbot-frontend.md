# Chatbot Frontend 설계

## 작업 리스트

- [x] `api.js`에 SSE 스트리밍 fetch 메서드 추가
- [x] `app.js`에 chat 상태 및 메서드 추가
- [x] `index.html` 포트폴리오 섹션에 챗 버블 + 패널 HTML 추가

## 배경

백엔드 챗봇 API(`POST /api/chat`, SSE 스트리밍)가 완성되었으므로, 포트폴리오 화면에서 사용할 프론트엔드 챗봇 UI를 추가한다.

## 핵심 결정

- **UI 형태**: 포트폴리오 화면 우측 하단 플로팅 채팅 버블 + 확장 패널
- **패널 크기**: w-96 (~384px), 높이 h-[500px]
- **표시 범위**: `currentPage === 'portfolio'`일 때만 표시
- **모드 전환**: 패널 상단 pill 버튼 (PORTFOLIO / FINANCIAL)
- **종목 선택**: FINANCIAL 모드에서 패널 내 종목 검색 드롭다운 (기존 `searchStocks` API 활용)
- **SSE 처리**: `fetch` + `ReadableStream` (POST 요청이므로 EventSource 사용 불가)
- **기술 스택**: Alpine.js 상태 확장 + Tailwind CSS (기존 패턴 유지)

## 구현

### api.js — SSE 스트리밍 메서드

기존 `request()` 메서드는 `response.json()`을 반환하므로, SSE 스트리밍용 별도 메서드 추가.

- `streamChat(userId, message, chatMode, stockCode, onChunk, onDone, onError)` 메서드 추가
- `fetch` + `reader.read()` 루프로 `data:` 라인 파싱
- 각 청크마다 `onChunk(text)` 콜백 호출
- 스트림 완료 시 `onDone()`, 에러 시 `onError(error)` 호출

[예시 코드](./examples/api-example.md)

### app.js — chat 상태 및 메서드

기존 `dashboard()` 함수의 반환 객체에 chat 관련 상태와 메서드 추가.

**상태:**
```javascript
chat: {
    isOpen: false,
    chatMode: 'PORTFOLIO',
    stockCode: null,
    stockName: null,
    stockSearchQuery: '',
    stockSearchResults: [],
    messages: [],
    inputText: '',
    isLoading: false
}
```

**메서드:**
- `toggleChat()` — 패널 열기/닫기
- `setChatMode(mode)` — PORTFOLIO/FINANCIAL 모드 전환, 종목 초기화
- `searchChatStocks()` — 종목 검색 (기존 `API.searchStocks` 활용)
- `selectChatStock(stock)` — 종목 선택
- `sendChatMessage()` — 메시지 전송 + SSE 스트리밍 처리
- `scrollChatToBottom()` — 메시지 영역 자동 스크롤

[예시 코드](./examples/app-example.md)

### index.html — 챗 버블 + 패널 HTML

포트폴리오 섹션(`x-show="currentPage === 'portfolio'"`) 내부 끝에 추가.

**구조:**
1. **챗 버블 버튼**: `fixed bottom-6 right-6`, 둥근 원형 버튼, 클릭 시 패널 토글
2. **챗 패널**: `fixed bottom-20 right-6 w-96 h-[500px]`, 카드 형태
   - 헤더: 타이틀 + 닫기 버튼
   - 모드 토글: pill 버튼 2개 (포트폴리오 분석 / 재무 분석)
   - 종목 선택: FINANCIAL 모드에서만 표시, 검색 input + 결과 드롭다운
   - 메시지 영역: `overflow-y-auto flex-1`, 사용자/AI 메시지 버블
   - 입력 영역: input + 전송 버튼

[예시 코드](./examples/html-example.md)

## 주의사항

- SSE 응답은 `text/event-stream` 형식이므로 `Content-Type: application/json` 헤더만 요청에 설정
- 스트리밍 중(`isLoading === true`) 전송 버튼 비활성화, 입력 잠금
- 종목 검색 결과 드롭다운은 `@click.outside`로 외부 클릭 시 닫기
- 메시지 추가 시 `$nextTick`으로 스크롤 보장