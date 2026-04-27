---
status: pending
priority: p2
issue_id: 009
tags: [code-review, api-consistency, validation, portfolio]
dependencies: []
---

# `MethodArgumentNotValidException` 핸들러 부재 — 응답 포맷 불일치

## Problem Statement

신규 매도 컨트롤러에 `@Valid`를 붙였지만 `GlobalExceptionHandler`에 `MethodArgumentNotValidException` 핸들러가 없다. Spring 기본 핸들러로 떨어져 400은 나오지만 기존 `IllegalArgumentException` → `{error: "BAD_REQUEST", message, timestamp}` 포맷과 응답 바디가 달라 클라이언트가 동일 키로 파싱 불가.

## Findings

- 위치: `infrastructure/web/GlobalExceptionHandler.java`
- `StockSaleRequest`/`StockSaleHistoryUpdateRequest`의 Bean Validation 위반 시:
  - 현재: Spring default `{timestamp, status, error, message, path}` 포맷
  - 기존 도메인 검증 실패: `{error: BAD_REQUEST, message, timestamp}`
- 클라이언트 `portfolio.js`의 alert는 `e.message`만 보여주는데, default 포맷에서는 message가 비어 사용자에게 빈 알림 노출

## Proposed Solutions

### Option A — `MethodArgumentNotValidException` 핸들러 추가
```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    String message = ex.getBindingResult().getFieldErrors().stream()
        .map(e -> e.getField() + ": " + e.getDefaultMessage())
        .collect(Collectors.joining(", "));
    return ResponseEntity.badRequest().body(new ErrorResponse("BAD_REQUEST", message, ...));
}
```

## Recommended Action

A 적용.

## Technical Details

- 영향 파일:
  - `src/main/java/com/thlee/stock/market/stockmarket/infrastructure/web/GlobalExceptionHandler.java`

## Acceptance Criteria

- [ ] Bean Validation 실패 시 `{error, message, timestamp}` 포맷 반환
- [ ] 클라이언트 alert에 의미 있는 메시지 표시
- [ ] 기존 IllegalArgumentException 응답 동일

## Work Log

- 2026-04-27: ce-review 발견 (architecture-strategist P2-5)