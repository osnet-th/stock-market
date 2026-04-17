# `API.streamChat()` SSE 파서 수정 예시

## 위치
`src/main/resources/static/js/api.js` — `streamChat` 메서드 내부 SSE 파싱 루프

## 변경 전 (버그)

```js
const reader = response.body.getReader();
const decoder = new TextDecoder();
let buffer = '';

while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split('\n');     // 라인 단위로만 분할 (이벤트 경계 없음)
    buffer = lines.pop();

    for (const line of lines) {
        if (line.startsWith('data:')) {
            const text = line.substring(5);
            if (text.trim()) {             // 빈 data 라인 필터링 → \n 소실
                onChunk(text);             // 여러 data 라인 간 \n 미삽입
            }
        }
    }
}

onDone();
```

## 변경 후 (수정안)

```js
const reader = response.body.getReader();
const decoder = new TextDecoder();
let buffer = '';

while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });

    // SSE 이벤트는 빈 줄(\n\n)로 구분
    const events = buffer.split('\n\n');
    buffer = events.pop(); // 마지막은 미완성 이벤트일 수 있으므로 버퍼에 남김

    for (const event of events) {
        if (!event) continue;

        // 한 이벤트 내의 모든 data: 라인을 모아 \n으로 join한 것이 payload
        const dataLines = [];
        for (const line of event.split('\n')) {
            if (line.startsWith('data:')) {
                // "data:..." 또는 "data: ..." 모두 허용 (뒤쪽 공백 한 칸 선택적으로 제거)
                let part = line.substring(5);
                if (part.startsWith(' ')) part = part.substring(1);
                dataLines.push(part);
            }
            // 그 외(event:, id:, retry:, 주석 등)는 현재 사용처 없음 → 무시
        }

        if (dataLines.length === 0) continue;

        const payload = dataLines.join('\n');
        onChunk(payload);
    }
}

onDone();
```

## 주요 포인트

- **이벤트 경계**: `\n\n`으로 이벤트 단위 분리. 버퍼 마지막 조각은 다음 read 루프에서 이어받음
- **data 라인 join**: 같은 이벤트에 속한 여러 `data:` 라인을 `\n`으로 join → 원본 개행 보존
- **빈 data 라인 포함**: `data:` 뒤가 빈 문자열이어도 `dataLines`에 넣음 (이게 원본 `\n`을 만들어 냄)
- **선두 공백 처리**: Spring 기본 포맷(`data:Hello` 또는 `data: Hello`) 양쪽 모두 안전하게 동작
- **`onChunk` 호출 단위**: 이벤트 1건당 1회 호출. 호출자 쪽 누적 로직(`content += chunk`) 변경 불필요

## 호출자 (수정 불필요)

`src/main/resources/static/js/components/chat.js`의 `sendChatMessage()`는 그대로 유지:
```js
(chunk) => {
    if (abortController.signal.aborted) return;
    this.chat.messages[assistantIdx].content += chunk; // \n 포함된 텍스트가 누적됨
    this.$nextTick(() => this.scrollChatToBottom());
},
```

누적된 `msg.content`에는 정상 마크다운 텍스트(헤더, 리스트, 개행 포함)가 들어가고, 기존 `renderMarkdown(msg.content)`(marked.parse)가 정상적으로 HTML로 변환한다.

## 회귀 확인 포인트

1. 헤더(`##`, `####`) · 리스트(`*`, `1.`) · 코드블록 포함 장문 응답이 구조대로 렌더링되는지
2. 짧은 한 줄 응답도 깨지지 않는지
3. 사용자가 응답 중 abort(패널 닫기/전송 재시도) 시 이상 없는지
4. 에러 경로(`onErrorResume(e -> Flux.just("오류가 발생했습니다: ..."))`)도 텍스트가 정상 표시되는지