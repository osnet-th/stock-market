---
title: "fix: AI 채팅 페이지 컨텍스트 초기화"
type: fix
status: active
date: 2026-04-08
origin: docs/brainstorms/2026-04-08-chat-page-context-reset-brainstorm.md
---

# fix: AI 채팅 페이지 컨텍스트 초기화

## Overview

ECOS 페이지에서 ECONOMIC 모드로 AI 채팅을 열고 포트폴리오 페이지로 이동하면, AI 채팅 버튼 클릭 시 이전 ECONOMIC 모드의 대화가 그대로 남아있는 문제를 수정한다.

**근본 원인:** `navigateTo()`에서 채팅 상태를 정리하지 않고, `toggleChat()`이 현재 페이지 컨텍스트를 고려하지 않음.

## 현재 동작 분석

| 파일 | 위치 | 현재 동작 | 문제 |
|------|------|-----------|------|
| `index.html` | :2258 | 채팅 버블 버튼이 `portfolio` 페이지에서만 표시 | ECOS 페이지에서는 버블 없음 (별도 "AI 분석" 버튼만 존재) |
| `chat.js` | :24-31 | `toggleChat()`이 단순 open/close만 수행 | 페이지 컨텍스트 무시, 이전 모드/대화 유지 |
| `app.js` | :104-148 | `navigateTo()`에서 채팅 상태 미처리 | 페이지 이동 후에도 채팅 열림 상태 유지 |

## Acceptance Criteria

- [x] 1. `navigateTo()` — 페이지 이동 시 채팅 닫기
- [x] 2. `toggleChat()` — 현재 페이지 기반 모드 초기화
- [x] 3. 채팅 버블 버튼 — ecos, portfolio 페이지에서만 표시
- [x] 4. 기존 ECOS "AI 분석" 버튼 (`openEcosChat()`) 동작 유지

## MVP

### 작업 1: `app.js` — `navigateTo()`에 채팅 닫기 로직 추가

`app.js:104` — `navigateTo()` 메서드 상단, `closeMobileDrawer()` 다음에 채팅 상태 정리 추가.

```javascript
// app.js navigateTo() 내부
async navigateTo(page) {
    this.closeMobileDrawer();

    // 페이지 이동 시 채팅 닫기 + 스트리밍 중단
    if (this.chat.isOpen) {
        if (this.chat._abortController) {
            this.chat._abortController.abort();
        }
        this.chat.isOpen = false;
        this.chat.isLoading = false;
    }

    // ... 기존 코드
}
```

### 작업 2: `chat.js` — `toggleChat()`에 페이지 기반 모드 초기화 로직 추가

`chat.js:24` — `toggleChat()` 메서드에서 채팅을 열 때 `currentPage`에 맞는 모드로 초기화.

```javascript
// chat.js toggleChat()
toggleChat() {
    this.chat.isOpen = !this.chat.isOpen;
    if (this.chat.isOpen) {
        // 현재 페이지에 맞는 모드로 초기화
        const modeMap = { ecos: 'ECONOMIC', portfolio: 'PORTFOLIO' };
        const targetMode = modeMap[this.currentPage] || 'PORTFOLIO';

        if (this.chat._abortController) {
            this.chat._abortController.abort();
        }
        this.chat.chatMode = targetMode;
        this.chat.messages = [];
        this.chat.inputText = '';
        this.chat.isLoading = false;
        this.chat.stockCode = null;
        this.chat.stockName = null;
        this.chat.stockSearchQuery = '';
        this.chat.stockSearchResults = [];
        this.chat.indicatorCategory = targetMode === 'ECONOMIC' ? this.ecos?.selectedCategory : null;
        this.chat.dragPos = { x: null, y: null };
    } else if (this.chat._cleanupDrag) {
        this.chat._cleanupDrag();
    }
},
```

### 작업 3: `index.html` — 채팅 버블 버튼 표시 조건 수정

`index.html:2258` — `x-show` 조건을 ecos, portfolio 양쪽에서 표시하도록 변경.

```html
<!-- 변경 전 -->
<button @click="toggleChat()"
        x-show="currentPage === 'portfolio'"

<!-- 변경 후 -->
<button @click="toggleChat()"
        x-show="currentPage === 'portfolio' || currentPage === 'ecos'"
```

### 작업 4: 기존 `openEcosChat()` 동작 유지 확인

`ecos.js:80` — `openEcosChat()`은 ECOS 페이지 내 "AI 분석" 버튼에서 호출되며, 이미 카테고리를 명시적으로 설정하므로 수정 불필요. 기존 동작 유지.

## Sources

- **Origin brainstorm:** [docs/brainstorms/2026-04-08-chat-page-context-reset-brainstorm.md](docs/brainstorms/2026-04-08-chat-page-context-reset-brainstorm.md)
- 채팅 버블 버튼: `index.html:2257-2268`
- 채팅 패널: `index.html:2271`
- toggleChat: `chat.js:24-31`
- navigateTo: `app.js:104-148`
- openEcosChat: `ecos.js:80-93`