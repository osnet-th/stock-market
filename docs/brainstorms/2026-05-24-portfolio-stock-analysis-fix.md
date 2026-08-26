---
title: "fix: portfolio stock analysis context and access control"
date: 2026-05-24
status: brainstorm
scope: portfolio, chatbot, stock financial frontend/backend
---

# fix: portfolio stock analysis context and access control

## Problem

Portfolio 화면의 주식 분석 기능은 두 갈래로 나뉘어 있다.

- 보유 자산 카드의 `재무상세` 슬라이드 패널
- floating chat의 `종목 분석` 모드

현재 두 기능이 같은 사용자/보유 주식 맥락을 공유하지 않고, 일부 백엔드 호출은 `userId` query parameter를 그대로 신뢰한다. 그 결과 아래 문제가 발생할 수 있다.

1. 인증된 사용자와 요청 `userId`가 다른 경우에도 포트폴리오/챗봇 컨텍스트 조회가 가능할 수 있다.
2. 챗봇 `종목 분석`은 미국 주식을 프론트에서 선택할 수 있지만 백엔드는 국내 DART 기반 분석만 수행한다.
3. 챗봇 분석은 포트폴리오 보유 수량, 평단, 투자금, 비중, 손익을 사용하지 않아 "내 보유 주식 분석"으로 보기 어렵다.
4. 국내 재무상세 패널은 현재 연도를 기본값으로 사용해, 아직 공시 데이터가 없는 연도에서 빈 결과가 기본 노출될 가능성이 높다.

## Current Behavior

### Portfolio API access control

- `PortfolioController.assertUserMatches`는 존재하지만 매도 API 일부에만 적용되어 있다.
- 포트폴리오 항목 목록, 등록, 수정, 삭제, 매수/납입/뉴스 토글 등의 엔드포인트는 `userId` query parameter를 그대로 서비스에 전달한다.
- `ChatController`도 `userId` query parameter를 그대로 `ChatRequest`에 담는다.

### Financial detail panel

- 포트폴리오 카드의 `재무상세` 버튼은 `country === 'KR' || country === 'US'`이고 ETF가 아닌 주식에 노출된다.
- KR은 DART API 메뉴를 사용한다.
- US는 SEC API 메뉴를 사용한다.
- 국내 기본 연도는 `new Date().getFullYear()`이다.

### Chat financial analysis

- 프론트는 검색 결과의 국가/거래소 제한 없이 종목을 선택할 수 있다.
- 백엔드 `ChatContextBuilder`는 `StockFinancialService`와 `ValuationMetricService`를 통해 DART 기반 재무계정/지표/가치평가만 조립한다.
- 분석 요청에는 `portfolioItemId`가 없고, 보유 자산 정보는 사용하지 않는다.

## Goals

- 인증된 사용자와 요청 `userId` 불일치 시 포트폴리오/챗봇 컨텍스트 접근을 차단한다.
- 포트폴리오 보유 주식에서 바로 분석을 요청할 수 있도록 보유 종목 컨텍스트를 분석 입력에 포함한다.
- 국내 주식은 기존 DART 분석을 유지하고, 미국 주식은 SEC 기반 분석 또는 명시적인 미지원 안내 중 하나로 일관되게 처리한다.
- 국내 재무상세 기본 조회가 공시 데이터가 없는 현재 연도에서 빈 결과로 시작하지 않도록 fallback을 둔다.
- 기존 정적 프론트 구조(Alpine.js + Tailwind)와 레이어 규칙을 유지한다.

## Non-goals

- 투자 추천 알고리즘을 새로 만든다.
- 신규 JS 라이브러리를 도입한다.
- 포트폴리오 Entity 구조를 변경한다.
- 외부 API 저장소나 캐시 구조를 전면 재설계한다.

## Candidate Approach

### Access control

- `PortfolioController`의 `userId` 기반 엔드포인트에 `@AuthenticationPrincipal Long jwtUserId`를 추가하고 `assertUserMatches(jwtUserId, userId)`를 호출한다.
- `ChatController`에도 동일한 검증을 추가한다.
- dev 환경의 anonymous/null principal은 기존 주석 계약대로 건너뛴다.

### Portfolio-aware analysis

- 프론트에서 포트폴리오 보유 주식의 `재무상세` 패널 또는 카드 액션에서 챗봇 FINANCIAL 모드를 열 수 있게 한다.
- 분석 요청 DTO에는 `portfolioItemId`를 선택적으로 추가한다.
- 백엔드 FINANCIAL 분석은 `portfolioItemId`가 있으면 `PortfolioService.getItems(userId)` 또는 dedicated 조회를 통해 해당 사용자의 보유 항목인지 검증하고, 보유 컨텍스트를 facts에 추가한다.
- public API 요청 필드 추가가 필요하므로 approval gate 대상이다.

### Country-specific financial facts

- KR: 기존 DART 기반 facts 유지.
- US: 이미 존재하는 `SecFinancialService`를 사용해 SEC 재무제표/투자지표 facts를 구성한다.
- 기타 국가/ETF: 프론트에서 분석 버튼을 숨기거나 백엔드에서 명확한 미지원 안내를 반환한다.

### Financial detail fallback

- 국내 상세 패널 기본 연도를 current year 대신 백엔드가 실제 데이터 있는 연도를 찾는 방식과 맞춘다.
- 최소 변경으로는 프론트 `getDefaultYear()`를 `currentYear - 1`로 변경하고, 데이터 없음 시 이전 연도 재조회 fallback을 적용한다.

## Risks

- `ChatRequest` 필드 추가는 프론트/백엔드 API 계약 변경이다.
- SEC facts를 챗봇에 넣으면 prompt 크기가 커질 수 있다.
- 포트폴리오 API 전체에 인증 검증을 추가하면 dev/prod principal 주입 방식 차이를 반드시 확인해야 한다.
- DART/SEC 외부 API 실패 시 분석 응답이 지나치게 빈 컨텍스트로 생성될 수 있다.

## Approval Gates

- `ChatRequest`/프론트 `streamChat` 요청 필드 변경은 public API 시그니처 변경에 해당한다.
- 포트폴리오/챗봇 userId 검증 강화는 비즈니스 동작 변경에 해당한다.
- US 종목 분석을 SEC로 확장하는 것은 기능 동작 변경에 해당한다.

구현 전 plan 승인 후 진행한다.
