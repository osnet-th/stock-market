---
status: pending
priority: p2
issue_id: 005
tags: [code-review, agent-native, chatbot, portfolio]
dependencies: []
---

# 챗봇 컨텍스트에 매도 이력 미주입 — 매도 회고 질문 답변 불가

## Problem Statement

`ChatContextBuilder.buildPortfolioContext()` (`chatbot/application/ChatContextBuilder.java:62-88`)가 보유 종목(ACTIVE)만 노출. 매도 이력은 챗봇 컨텍스트에 전혀 들어가지 않음.

## Findings

- "최근 매도 회고해줘", "이번 분기 실현손익은?", "왜 OO를 팔았어?" 같은 질문에 LLM이 답할 근거 없음
- `getItems`는 ACTIVE 항목만 반환(`PortfolioItemRepositoryImpl.java:38-39`) → CLOSED 종목은 흔적조차 없음
- ROI 측면에서 한 군데 보강이 챗봇 가치 즉각 상승

## Proposed Solutions

### Option A — `PORTFOLIO` 모드에 매도 이력 섹션 추가
- `portfolioService.getAllUserSaleHistories(userId)` 결과를 "## 최근 매도 이력" 섹션으로 주입(최근 N건)
- N은 토큰 한도 고려하여 10~20건

### Option B — `ChatMode.SALE_REVIEW` 신규 모드
- 매도 회고 전용 모드 추가
- Pros: 토큰 절약 / Cons: 모드 분기 추가

## Recommended Action

A 권장 (작은 변경, 큰 가치).

## Technical Details

- 영향 파일:
  - `src/main/java/com/thlee/stock/market/stockmarket/chatbot/application/ChatContextBuilder.java`

## Acceptance Criteria

- [ ] PORTFOLIO 모드에서 매도 이력이 컨텍스트에 포함됨
- [ ] 토큰 한도 내 (최근 N건 cap)
- [ ] 매도 회고 질문 정상 응답

## Work Log

- 2026-04-27: ce-review 발견 (agent-native-reviewer P1)