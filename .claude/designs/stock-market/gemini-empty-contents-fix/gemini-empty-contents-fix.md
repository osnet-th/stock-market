# Gemini API 400 에러 수정 - 분석 버튼 클릭 시 빈 contents 전송 문제

## 작업 리스트

- [x] ChatService에서 분석 요청 시 기본 사용자 메시지 생성 로직 추가
- [x] GeminiRequest.of()에서 contents 빈 배열 방어 로직 추가

## 문제 분석

### 현상
챗봇 FINANCIAL 모드에서 종목 분석 버튼(저평가/고평가 판단 등) 클릭 시 Gemini API 400 Bad Request 발생.

### 원인

분석 버튼 클릭 시 요청 흐름:

1. **프론트엔드** (`chat.js:107`): `sendChatMessage({ message: '', analysisTask: task })` — message가 빈 문자열
2. **ChatService** (`ChatService.java:29`): `allMessages.add(new ChatMessage("user", request.message()))` — 빈 문자열 메시지 추가
3. **GeminiRequest.of()** (`GeminiRequest.java:23`): `.filter(m -> m.content() != null && !m.content().isBlank())` — 빈 메시지 필터링
4. 첫 분석 요청 시 히스토리도 없으므로 → **`contents`가 빈 배열 `[]`**
5. Gemini API는 `contents`에 최소 1개 메시지 필수 → **400 Bad Request**

### 영향 범위
- FINANCIAL 모드의 모든 분석 버튼 (저평가/고평가, 실적 추세, 리스크 진단, 투자 적정성)
- 특히 첫 분석 요청(히스토리 없음) 시 100% 재현
- 히스토리가 있는 경우에도 이전 메시지가 모두 빈 문자열이면 동일 문제 발생 가능

## 핵심 결정

- **수정 위치**: `ChatService` — 분석 요청 시 빈 메시지 대신 분석 작업에 맞는 기본 메시지 생성
- **방어 로직 추가**: `GeminiRequest.of()` — contents가 비더라도 최소 1개 메시지 보장
- **프론트엔드 변경 없음**: 프론트엔드는 현재 동작 유지 (analysisTask로 의도 전달, 표시용 텍스트는 별도 관리)

## 구현

### ChatService 수정
위치: `chatbot/application/ChatService.java:20-32`

분석 요청(`analysisTask != null`)일 때 빈 message 대신 분석 작업명을 사용자 메시지로 변환하여 추가.

[예시 코드](./examples/application-service-example.md)

### GeminiRequest 방어 로직
위치: `chatbot/infrastructure/gemini/dto/GeminiRequest.java:21-32`

contents가 빈 배열일 경우 기본 사용자 메시지를 추가하는 방어 로직.

[예시 코드](./examples/infrastructure-dto-example.md)

## 주의사항

- system_instruction에 이미 분석 지시와 재무 데이터가 포함되어 있으므로, 사용자 메시지는 분석 요청 의도만 간결히 전달하면 충분
- 프론트엔드의 `displayText` 로직(`chat.js:145-147`)은 변경하지 않음 (UI 표시용)
