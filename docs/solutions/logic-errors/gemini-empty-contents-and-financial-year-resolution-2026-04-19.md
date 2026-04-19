---
title: "Gemini API 400 Bad Request and Financial Data Year Resolution in Chatbot Analysis"
date: 2026-04-19
category: logic-errors
module: chatbot
problem_type: logic_error
component: service_object
severity: high
symptoms:
  - "Gemini API returns 400 Bad Request when clicking stock analysis buttons"
  - "Empty contents array sent to Gemini API because requestAnalysis() sends empty message string"
  - "Financial data queries return empty accounts and N/A valuation metrics (EPS, BPS, PER, PBR)"
  - "DART annual report lookup uses current year (2026) but reports for 2026 do not exist yet"
root_cause: logic_error
resolution_type: code_fix
tags:
  - gemini-api
  - chatbot
  - financial-analysis
  - year-fallback
  - dart-api
  - empty-contents
---

# Gemini API 400 Bad Request and Financial Data Year Resolution in Chatbot Analysis

## Problem

FINANCIAL 모드에서 분석 버튼 클릭 시 Gemini API 400 Bad Request 오류가 발생하고, API 호출이 성공하더라도 재무 데이터 연도 해석 오류로 인해 AI가 핵심 지표(EPS, BPS, PER, PBR)를 모두 N/A로 인식하여 분석을 수행하지 못하는 복합 장애가 발생했다.

## Symptoms

- 분석 버튼(저평가/고평가 판단, 실적 추세 요약, 리스크 진단, 투자 적정성 의견) 클릭 시 400 Bad Request 응답
- Gemini API 에러 로그: `400 Bad Request from POST https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:streamGenerateContent`
- AI 응답에서 "핵심 데이터가 누락되어 분석이 불가합니다" 메시지 출력
- 재무제표 계정 항목이 "데이터 없음"으로 표시
- 지수 데이터가 3년치 대신 1-2년치만 조회됨
- EPS, BPS, PER, PBR 값이 모두 N/A

## What Didn't Work

초기 조사에서는 Gemini API 400 에러만 확인하여 빈 contents 배열 문제에만 집중했다. 프론트엔드에서 `requestAnalysis()`가 빈 문자열 메시지를 전송하는 것을 확인하고 이를 수정했으나, API 호출 성공 후에도 AI가 분석을 거부하는 현상이 계속되었다. 이후 AI에게 전달되는 컨텍스트 데이터 자체를 검증한 결과, 연도 해석 로직이 현재 연도(2026)를 기준으로 데이터를 조회하고 있어 아직 공시되지 않은 2026년 사업보고서를 찾으려 시도하는 두 번째 근본 원인을 발견했다.

## Solution

### Fix 1: 빈 contents 배열 방지

**AnalysisTask.java** — 분석 태스크별 사용자 메시지 생성:

```java
// Before: 프론트엔드가 빈 message를 전송하면 contents가 빈 배열이 됨

// After:
public String toUserMessage() {
    return switch (this) {
        case UNDERVALUATION -> "이 종목의 저평가/고평가 여부를 판단해주세요.";
        case TREND_SUMMARY -> "이 종목의 실적 추세를 요약해주세요.";
        case RISK_DIAGNOSIS -> "이 종목의 리스크 요인을 진단해주세요.";
        case INVESTMENT_OPINION -> "이 종목의 투자 적정성 의견을 제시해주세요.";
    };
}
```

**ChatService.java** — 빈 메시지를 태스크 메시지로 대체:

```java
// Before:
allMessages.add(new ChatMessage("user", request.message()));

// After:
String userMessage = resolveUserMessage(request);
allMessages.add(new ChatMessage("user", userMessage));

private String resolveUserMessage(ChatRequest request) {
    if (request.message() != null && !request.message().isBlank()) {
        return request.message();
    }
    if (request.analysisTask() != null) {
        return request.analysisTask().toUserMessage();
    }
    return "";
}
```

**GeminiRequest.java** — 방어적 폴백:

```java
// Before: contents가 빈 배열이면 그대로 API 전송

// After:
private static final Content FALLBACK_USER_CONTENT =
    new Content("user", List.of(new Part("위 시스템 지시사항에 따라 분석해주세요.")));

public static GeminiRequest of(String systemPrompt, List<ChatMessage> messages) {
    List<Content> contents = messages.stream()
            .filter(m -> m.content() != null && !m.content().isBlank())
            .map(m -> new Content(m.role(), List.of(new Part(m.content()))))
            .toList();

    if (contents.isEmpty()) {
        contents = List.of(FALLBACK_USER_CONTENT);
    }
    // ...
}
```

### Fix 2: 재무 데이터 연도 해석 보정

**ChatContextBuilder.java** 및 **ValuationMetricService.java** — 유효 연도 해석 로직:

```java
// Before:
int currentYear = LocalDate.now().getYear(); // 2026 → 데이터 없음

// After:
private int resolveEffectiveYear(String stockCode) {
    int currentYear = LocalDate.now().getYear();
    try {
        List<FinancialAccountResponse> accounts = stockFinancialService.getFinancialAccounts(
                stockCode, String.valueOf(currentYear), REPORT_CODE_ANNUAL);
        if (!accounts.isEmpty()) {
            return currentYear;
        }
    } catch (Exception e) {
        log.debug("당해연도 재무계정 조회 실패, 전년도 시도: stockCode={}, year={}", stockCode, currentYear);
    }
    return currentYear - 1;
}
```

## Why This Works

**Bug 1**: 프론트엔드의 분석 버튼은 설계상 사용자 입력 메시지 없이 `analysisTask`만 전송한다. `resolveUserMessage()`가 빈 메시지를 태스크에 맞는 구체적인 질문으로 변환하여 Gemini API의 최소 1개 메시지 요구사항을 충족시킨다. `GeminiRequest`의 폴백은 예상치 못한 경로로 빈 contents가 만들어질 경우를 대비한 방어 계층이다.

**Bug 2**: DART 사업보고서(보고서코드 "11011")는 회계연도 종료 후 약 3개월 뒤(3월경) 공시된다. 2026년 4월 시점에서 2026년 사업보고서는 존재하지 않는다. `resolveEffectiveYear()`가 현재 연도 데이터 존재 여부를 먼저 확인하고, 없으면 전년도로 폴백하여 실제 존재하는 최신 데이터를 기준으로 분석한다.

## Prevention

- **API 계약 검증**: 외부 API 호출 전 필수 파라미터(contents 비어있지 않음 등)를 검증하는 가드 로직을 API 클라이언트 레이어에 배치한다.
- **시간 의존 로직 추상화**: `LocalDate.now()`를 직접 사용하는 대신, 데이터 가용성을 확인하는 해석 계층을 둔다. 특히 외부 데이터 소스(DART 공시 등)의 공시 일정과 실제 데이터 존재 시점의 차이를 고려한 폴백 전략을 설계 단계에서 반영한다.
- **프론트엔드-백엔드 메시지 계약 명확화**: 사용자 입력 없이 발생하는 요청 경로를 식별하고, 백엔드에서 해당 경로의 메시지를 자체 생성하는 책임을 명시적으로 설계한다.

## Related Issues

- Design doc: `.claude/designs/stock-market/gemini-empty-contents-fix/gemini-empty-contents-fix.md`
- Design doc: `.claude/designs/stock-market/financial-year-fallback/financial-year-fallback.md`