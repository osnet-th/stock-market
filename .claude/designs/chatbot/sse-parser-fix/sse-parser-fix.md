# sse-parser-fix 설계

## 작업 리스트

- [x] `API.streamChat()`의 SSE 파서를 이벤트 경계 기반으로 수정
- [x] 한 이벤트 내 여러 `data:` 라인을 `\n`으로 join하여 `onChunk`에 전달
- [ ] 브라우저에서 삼성생명 분석 등 장문 응답으로 마크다운 렌더링 확인

## 배경

챗봇 응답의 `\n`이 프론트엔드 SSE 파서에서 소실되어 마크다운이 한 줄로 표시됨. 원인·증상은 `.claude/analyzes/chatbot/sse-newline-stripped/sse-newline-stripped.md` 참조.

## 핵심 결정

- **수정 범위는 프론트엔드 파서 한 곳만**: 백엔드(Spring WebFlux, `GeminiAdapter`)는 SSE 스펙대로 동작 중이므로 변경하지 않음
- **SSE 스펙 준수**: 이벤트 경계는 `\n\n`, 한 이벤트 내의 여러 `data:` 라인은 `\n`으로 join한 값이 해당 이벤트의 data
- **빈 `data:` 라인을 필터링하지 않음**: 원본 텍스트의 개행을 나타내는 정상 신호이므로 제거 금지
- **`onChunk` 호출 단위는 "이벤트 1건 = 호출 1회"**: 호출자(`ChatComponent.sendChatMessage`)의 누적 로직은 변경 불필요

## 구현

### `API.streamChat()` SSE 파서 수정

위치: `src/main/resources/static/js/api.js` (`streamChat` 메서드 내부 SSE 파싱 루프)

변경 요점:
- `buffer.split('\n\n')`로 이벤트 단위 분리(기존 유지)
- 이벤트 하나마다 `data:` 라인을 모아 **`\n`으로 join**한 결과를 단일 `onChunk(text)` 호출로 전달
- `text.trim()` 기반 필터링 제거
- `data:` 뒤의 선택적 공백 한 칸(`data: ...`) 처리 유지 (Spring 기본 포맷 대응)

[구현 예시](./examples/api-stream-chat-example.md)

## 주의사항

- **호출자 쪽은 수정 금지**: `ChatComponent.sendChatMessage`의 `content += chunk` 로직은 그대로 유지 (onChunk에 이미 `\n` 포함된 텍스트가 전달됨)
- **에러 메시지 브랜치 호환**: `data:` 로 시작하는 에러 문자열도 동일 경로로 전달되어야 하므로 `data:` 프리픽스 제거만 하고 원문은 보존
- **스트리밍 UX 영향 없음**: 이벤트 단위 delivery이므로 기존 점진적 표시 동작은 유지됨
- **회귀 테스트 포인트**: (1) 헤더/리스트/코드블록 포함 장문 응답, (2) 짧은 한 줄 응답, (3) 중간에 끊기는 abort 시나리오, (4) 에러 응답(`onErrorResume` 경로)