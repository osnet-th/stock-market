---
title: "fix: portfolio stock analysis context and access control"
date: 2026-05-24
status: planned
origin: docs/brainstorms/2026-05-24-portfolio-stock-analysis-fix.md
scope: portfolio, chatbot, stock financial frontend/backend
---

# fix: portfolio stock analysis context and access control

## Summary

Portfolio의 주식 분석 기능을 보유 종목 기준으로 일관되게 동작하도록 수정한다. 우선 `userId` query parameter 신뢰 문제를 막고, 챗봇 `종목 분석`에 보유 주식 컨텍스트를 전달하며, 국내/미국 재무 데이터 경로의 불일치를 정리한다.

## Scope

- `PortfolioController` userId access control 적용 범위 확대
- `ChatController` userId access control 추가
- `ChatRequest`에 선택적 `portfolioItemId` 추가
- 프론트 `streamChat` 요청에 선택적 `portfolioItemId` 전달
- 포트폴리오 보유 주식에서 챗봇 종목 분석을 바로 시작하는 UI 메서드 추가
- `ChatContextBuilder`에 보유 주식 컨텍스트 추가
- FINANCIAL 분석의 KR/US 분기 처리
- 국내 재무상세 기본 연도/fallback 개선

## Out of Scope

- Entity 생성/수정
- 신규 JS 라이브러리 도입
- 투자 판단 모델 신규 설계
- DART/SEC 외부 API 어댑터 전면 재작성

## Design

### 1. Access Control

- `PortfolioController`의 `@RequestParam Long userId` 엔드포인트에 `@AuthenticationPrincipal Long jwtUserId`를 추가한다.
- 각 핸들러 시작 지점에서 `assertUserMatches(jwtUserId, userId)`를 호출한다.
- `ChatController.chat`도 동일하게 `@AuthenticationPrincipal Long jwtUserId`를 받고 요청 `userId`와 비교한다.
- dev 환경에서 principal이 null이면 기존 방식대로 통과한다.

### 2. Portfolio-aware Chat Analysis

- `ChatRequest`와 `ChatMessageRequest`에 `Long portfolioItemId`를 선택 필드로 추가한다.
- `API.streamChat` 인자와 request body에 `portfolioItemId`를 추가한다.
- `ChatComponent` 상태에 `portfolioItemId`를 추가한다.
- 포트폴리오 주식 카드 또는 재무상세 패널에서 `openChatForPortfolioStockAnalysis(item)` 같은 메서드로 다음 상태를 설정한다.
  - `chat.isOpen = true`
  - `chat.chatMode = 'FINANCIAL'`
  - `chat.stockCode = item.stockDetail.stockCode`
  - `chat.stockName = item.itemName`
  - `chat.portfolioItemId = item.id`

### 3. Backend Facts

- `ChatContextBuilder.buildFinancialAnalysis`에서 `portfolioItemId`가 있으면 해당 user의 보유 항목인지 확인한다.
- 보유 컨텍스트에는 최소 아래 값을 포함한다.
  - 종목명, 종목코드, 국가, 거래소
  - 보유 수량
  - 평균 매수가
  - 투자 원금 원화 기준
  - 현재 평가금/손익은 기존 가격 조회가 안정적인 경우에만 포함
- `stockDetail.country`가 `KR`이면 기존 DART facts를 사용한다.
- `stockDetail.country`가 `US`이면 `SecFinancialService` facts를 사용한다.
- `portfolioItemId` 없이 `stockCode`만 들어온 경우는 기존 검색 기반 분석을 유지하되, KR 이외는 미지원 또는 SEC 가능 여부를 명확히 처리한다.

### 4. Frontend Financial Detail Fallback

- 국내 `openStockDetail` 기본 연도는 current year보다 current year - 1을 우선한다.
- `loadSelectedFinancial`에서 accounts 조회 결과가 비어 있고 기본 연도인 경우, 이전 연도로 1회 fallback한다.
- fallback 여부를 사용자에게 과도하게 노출하지 않고 결과 기준 연도를 select 값에 반영한다.

### 5. Error and Unsupported Cases

- ETF는 기존처럼 재무상세/분석 대상에서 제외한다.
- KR/US 외 국가는 분석 버튼을 숨기거나 미지원 안내를 표시한다.
- 외부 API 실패 시 빈 facts로 투자 의견을 생성하지 않고, 조회 실패 항목을 facts에 명시한다.

## Checklist

- [x] `PortfolioController` access control 적용 범위 확대
- [x] `ChatController` access control 추가
- [x] `ChatRequest`/`ChatMessageRequest`에 `portfolioItemId` 추가
- [x] `API.streamChat` 및 `ChatComponent`에 `portfolioItemId` 전달 추가
- [x] 포트폴리오 보유 주식에서 FINANCIAL 챗 분석을 시작하는 프론트 액션 추가
- [x] `ChatContextBuilder`에 보유 주식 facts 추가
- [x] `ChatContextBuilder`에 KR/US facts 분기 추가
- [x] 국내 재무상세 기본 연도/fallback 개선
- [x] code convention 관점에서 긴 메서드 분리 검토
- [x] 하네스 실행: `scripts/run-harness-checks.sh local-documented`
- [x] 필요 시 Gradle 테스트 또는 컴파일 검증 실행

## Verification

- 정적 하네스:
  - `scripts/run-harness-checks.sh local-documented`
- 백엔드:
  - `./gradlew test` 또는 최소 `./gradlew compileJava`
- 프론트:
  - 포트폴리오 페이지에서 보유 KR 주식 `재무상세` 조회
  - 보유 KR 주식에서 챗봇 `종목 분석` 요청
  - 보유 US 주식에서 SEC 상세 조회 및 챗봇 분석 동작 확인
  - 다른 `userId` query parameter 요청이 운영 인증 환경에서 차단되는지 확인

## Verification Results

- `git diff --check`: pass
- `./gradlew compileJava`: pass
- `scripts/run-harness-checks.sh local-documented`: fail
  - reason: 현재 작업공간이 primary worktree이며, documented workflow 하네스가 linked worktree 실행을 요구한다.
- `ALLOW_PRIMARY_DOCUMENTED=1 scripts/run-harness-checks.sh local-documented`: pass
  - reason: 샌드박스의 Git ref lock 생성 제한 때문에 primary documented escape hatch를 명시적으로 사용했다.

## Approval Required Before Work

이 plan은 아래 approval gate를 포함한다.

- public API 요청 필드 추가: `portfolioItemId`
- `userId` 검증 강화로 인한 동작 변경
- 미국 주식 챗봇 분석의 SEC facts 추가
- 포트폴리오 보유 주식 컨텍스트를 LLM 분석 facts에 포함

사용자 승인 후 구현을 시작한다.
