# sse-newline-stripped 분석

## 현재 상태

챗봇 응답이 UI에서 줄바꿈, 헤더, 리스트 등 마크다운 형식이 전혀 적용되지 않은 **한 줄 텍스트**로 표시됨.

- 백엔드: Gemini SSE → `GeminiAdapter.stream()` → `Flux<String>` → Spring WebFlux `TEXT_EVENT_STREAM_VALUE`로 인코딩
- 프론트엔드: `API.streamChat()`이 `fetch` 스트림을 읽어 `data:` 라인을 파싱 → `ChatComponent.sendChatMessage()`의 `onChunk` 콜백이 `assistant` 메시지에 누적 → `renderMarkdown(msg.content)`(marked.parse)으로 표시

## 문제점

- **문제 1**: Gemini가 내려주는 텍스트 청크(예: `"## 삼성생명\n\n#### 1. 기업 개요"`)의 `\n`이 프론트엔드에서 전부 소실됨
- **문제 2**: 결과적으로 `marked.parse`가 헤더/리스트/문단 구분을 인식하지 못해 한 줄의 평문처럼 렌더링됨

## 원인 분석

### 근본 원인: SSE 스펙과 프론트엔드 파서의 불일치

**SSE 스펙** (HTML Living Standard):
- 이벤트는 빈 줄(`\n\n`)로 구분
- **하나의 이벤트 안에 `data:` 라인이 여러 개 있으면 `\n`으로 join한 값**이 해당 이벤트의 data가 됨

**Spring WebFlux의 기본 SSE 인코더 동작**:
- `Flux<String>`의 원소에 `\n`이 있으면, 각 줄마다 `data:` prefix를 붙여서 송출
- 예: `"## 삼성생명\n\n#### 1. 기업 개요"` →
  ```
  data:## 삼성생명
  data:
  data:#### 1. 기업 개요

  ```

**프론트엔드 파서** (`src/main/resources/static/js/api.js:357-377`):
```js
const reader = response.body.getReader();
const decoder = new TextDecoder();
let buffer = '';

while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split('\n');      // ← 이벤트 경계(\n\n) 개념 없이 단일 \n로 분할
    buffer = lines.pop();

    for (const line of lines) {
        if (line.startsWith('data:')) {
            const text = line.substring(5);
            if (text.trim()) {              // ← 빈 data 라인 필터링 → 원본의 \n 소실
                onChunk(text);              // ← 여러 data 라인을 \n으로 join하지 않음
            }
        }
    }
}
```

- SSE 이벤트 경계(`\n\n`) 인식 없이 **라인 단위로만** 파싱
- 같은 이벤트에 속한 여러 `data:` 라인을 **독립적으로 개별 처리**
- 빈 `data:`(원본 `\n`을 나타내는 라인)를 `text.trim()`으로 **필터링** → 개행 신호 소실
- 필터링 살아남은 라인들도 **구분자 없이 이어붙임** → `\n`이 재구성되지 않음

→ 마크다운의 모든 개행이 사라져 한 줄짜리 텍스트가 됨.

## 영향 범위

- `src/main/resources/static/js/api.js:335-383` — `streamChat()` SSE 파서 로직
- 챗봇 응답이 표시되는 모든 화면 (포트폴리오/경제지표/종목 분석 모드 전체)

## 해결 방안

### 방안 A: 프론트엔드 SSE 파서를 스펙대로 수정 (권장)

- 이벤트 경계(`\n\n`)로 버퍼를 분할 후, 각 이벤트 내부의 모든 `data:` 라인을 **`\n`으로 join**하여 하나의 청크로 `onChunk`에 전달
- `text.trim()` 필터링 제거 (빈 `data:` 라인은 SSE 스펙상 정상 — 개행을 나타내는 신호)
- 장점: 백엔드 무변경, SSE 표준 준수, 변경 범위가 작음
- 단점: 파서 로직이 약간 길어짐

### 방안 B: 백엔드에서 `\n` 이스케이프 후 프론트에서 복원

- 장점: 파서 단순
- 단점: SSE 스펙 우회(해킹), 백엔드·프론트 양쪽 수정 필요, 향후 유지보수 혼란

### 방안 C: JSON SSE 포맷 전환 (`Flux<ServerSentEvent<Payload>>`)

- 장점: 구조적으로 깔끔, 메타데이터 확장 용이
- 단점: 변경 범위 큼, 당장의 버그 수정에 과함 (YAGNI 위배)

## 권장 사항

**방안 A** 채택. 문제의 근원이 프론트엔드 파서의 SSE 스펙 해석 오류이므로, 원인 지점에서만 수정하여 최소 변경으로 해결. 설계 문서: `.claude/designs/chatbot/sse-parser-fix/sse-parser-fix.md`.