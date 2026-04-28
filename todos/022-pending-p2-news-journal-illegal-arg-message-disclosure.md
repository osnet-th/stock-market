---
status: pending
priority: p2
issue_id: 022
tags: [code-review, security, error-handling, newsjournal]
dependencies: []
---

# `NewsJournalExceptionHandler` 가 `IllegalArgumentException` 메시지 무가공 노출

## Problem Statement

`IllegalArgumentException` 핸들러가 `e.getMessage()` 를 그대로 응답 바디 `message` 필드에 담는다. 도메인 검증 메시지 외에도 Hibernate / JSON 파서 / 외부 라이브러리에서 발생하는 동일 예외가 본 핸들러로 매핑될 경우, 내부 필드명·검증 로직 구조가 클라이언트로 누설된다. stocknote 도 동일 패턴이지만 본 변경의 일관 정책상 함께 다듬는 것이 안전.

## Findings

- 위치: `src/main/java/com/thlee/stock/market/stockmarket/newsjournal/presentation/NewsJournalExceptionHandler.java:36-40`
- `@RestControllerAdvice(assignableTypes = NewsJournalController.class)` 로 스코프 한정되어 다른 컨트롤러에는 영향 없음. 그러나 Controller 가 호출하는 application/domain/infrastructure 어디서 던진 `IllegalArgumentException` 도 본 핸들러가 잡음.
- 도메인 메시지(`"미래 날짜로 기록할 수 없습니다."` 등)는 사용자 친화 — 노출 의도. 라이브러리 메시지는 의도되지 않음.

## Proposed Solutions

### Option A — 도메인 전용 예외 신설 (Recommended)
- `NewsEventValidationException extends RuntimeException` 신설 (application/exception).
- `NewsEvent.create / updateBody / NewsEventLink.create` 의 `IllegalArgumentException` 을 `NewsEventValidationException` 으로 교체.
- 핸들러: `NewsEventValidationException` → 400 + e.getMessage() 노출, `IllegalArgumentException` → 400 + 정형 메시지("잘못된 요청입니다.").

### Option B — 핸들러에서 메시지 마스킹
- 도메인이 던지는 메시지를 식별할 단서 부재. 모든 `IllegalArgumentException` 을 정형 문구로 응답 → 사용자 친화 메시지가 사라져 UX 저하.

### Option C — 현 상태 유지
- stocknote 와 동일 패턴이라 일관성. 단점은 위 위험.

## Recommended Action

A. 분석 → 설계 → 승인 후 적용.

## Technical Details

- 신규 예외: `application/exception/NewsEventValidationException.java`
- 변경 파일: `NewsEvent.java`, `NewsEventLink.java`, `NewsJournalExceptionHandler.java`
- 영향 범위: 도메인 단위 테스트 메시지 비교 시 예외 타입 변경 반영 필요.

## Acceptance Criteria

- [ ] 도메인이 던지는 검증 실패 → `NewsEventValidationException` → 400 + 한국어 사용자 메시지
- [ ] 라이브러리에서 발생한 `IllegalArgumentException` → 400 + "잘못된 요청입니다." 정형
- [ ] 응답 shape `{error, message, timestamp}` 유지

## Work Log

- 2026-04-28: 발견 (ce-review 보안 P2-1)

## Resources

- security-sentinel 보고 P2-1
- 비교: `src/main/java/com/thlee/stock/market/stockmarket/stocknote/presentation/StockNoteExceptionHandler.java`