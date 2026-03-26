# Brainstorm: 챗봇 프론트엔드 UI

**Date**: 2026-03-25
**Status**: Decided

## What We're Building

포트폴리오 화면 우측 하단에 떠있는 채팅 버블 + 확장 가능한 챗 패널.
두 가지 모드(PORTFOLIO / FINANCIAL)를 챗 패널 내에서 전환하며, FINANCIAL 모드에서는 종목 검색/선택 드롭다운을 패널 내에 배치.

## Why This Approach

- 포트폴리오 화면을 보면서 동시에 질문 가능 (컨텍스트 유지)
- 기존 UI를 변경하지 않고 오버레이 형태로 추가 (영향 최소화)
- 우측 하단 채팅 버블은 사용자에게 익숙한 UX 패턴

## Key Decisions

| 항목 | 결정 |
|------|------|
| UI 형태 | 우측 하단 플로팅 채팅 버블 + 확장 패널 |
| 표시 범위 | 포트폴리오 화면에서만 (`currentPage === 'portfolio'`) |
| 패널 크기 | 중형 (w-96, ~384px), 높이 약 500px |
| 모드 전환 | 챗 패널 상단에 PORTFOLIO / FINANCIAL 토글 버튼 |
| 종목 선택 | FINANCIAL 모드에서 챗 패널 내 종목 검색 드롭다운 |
| SSE 스트리밍 | `fetch` + `ReadableStream` (EventSource는 POST 미지원) |
| 기술 스택 | Alpine.js 상태 확장 + Tailwind CSS (기존 패턴 유지) |

## Design Details

### 챗 패널 레이아웃

```
┌──────────────────────────┐
│ 💬 AI 어시스턴트    [✕]  │  ← 헤더 (타이틀 + 닫기)
├──────────────────────────┤
│ [포트폴리오] [재무분석]  │  ← 모드 토글 (pill 버튼)
│ [종목 검색 드롭다운    ] │  ← FINANCIAL 모드에서만 표시
├──────────────────────────┤
│                          │
│  채팅 메시지 영역        │  ← 스크롤 가능, 자동 스크롤
│  (사용자/AI 메시지)      │
│                          │
├──────────────────────────┤
│ [메시지 입력...] [전송]  │  ← 입력 + 전송 버튼
└──────────────────────────┘
```

### 상태 구조

```javascript
chat: {
  isOpen: false,         // 패널 열림/닫힘
  chatMode: 'PORTFOLIO', // 'PORTFOLIO' | 'FINANCIAL'
  stockCode: null,       // FINANCIAL 모드 선택 종목
  stockSearchQuery: '',  // 종목 검색어
  stockSearchResults: [],// 검색 결과
  messages: [],          // { role: 'user'|'assistant', content: '' }
  inputText: '',         // 현재 입력 중인 텍스트
  isLoading: false       // 스트리밍 중 여부
}
```

### SSE 스트리밍 처리

POST 요청이므로 `EventSource` 대신 `fetch` + `ReadableStream`으로 SSE 파싱:
- `fetch('/api/chat?userId=...', { method: 'POST', body: JSON.stringify({...}) })`
- `response.body.getReader()` → `TextDecoder` → `data:` 라인 파싱
- 스트리밍 중 messages 마지막 항목에 텍스트 append

### 파일 변경 범위

| 파일 | 변경 내용 |
|------|-----------|
| `index.html` | 포트폴리오 섹션에 챗 버블 + 패널 HTML 추가 |
| `js/app.js` | chat 상태, 메서드 (toggleChat, sendMessage, selectStock 등) 추가 |
| `js/api.js` | sendChatMessage SSE fetch 메서드 추가 |

## Open Questions

_없음 — 모든 핵심 결정 완료_