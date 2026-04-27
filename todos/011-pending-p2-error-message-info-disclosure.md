---
status: pending
priority: p2
issue_id: 011
tags: [code-review, security, info-disclosure, portfolio]
dependencies: []
---

# 도메인 IllegalArgumentException 메시지가 그대로 응답 — 정보 노출

## Problem Statement

`GlobalExceptionHandler.java:51-54`가 도메인/검증 메시지를 `BAD_REQUEST.message`로 직접 반환. 매도 흐름의 "연결된 원화 항목을 찾을 수 없습니다.", "이미 마감된 항목은 매도할 수 없습니다." 등이 그 자체로 **존재성/상태 oracle**로 노출(IDOR 결합 시 다른 사용자의 itemId가 STOCK인지/CLOSED인지 알아낼 수 있음).

## Findings

- 위치: `infrastructure/web/GlobalExceptionHandler.java:51-54`
- todo 002(IDOR) 결합 시 위협 증가
- `ObjectOptimisticLockingFailureException` 핸들러(L70-74)는 generic 메시지로 안전

## Proposed Solutions

### Option A — 도메인 검증 메시지 ↔ 권한 검증 메시지 분리
- `IllegalArgumentException` 중 권한/소유권 관련은 별도 예외 타입(`UnauthorizedAccessException` 등) 도입
- 권한 예외는 `404 Not Found` 또는 generic "요청을 처리할 수 없습니다"로 통일

### Option B — 모든 메시지를 generic으로 통일
- Pros: 단순 / Cons: 디버깅/UX 손해

## Recommended Action

A. (todo 002와 함께 처리하면 효율적)

## Technical Details

- 영향 파일:
  - `src/main/java/com/thlee/stock/market/stockmarket/infrastructure/web/GlobalExceptionHandler.java`
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioService.java` (예외 타입 분리)

## Acceptance Criteria

- [ ] 권한/소유권 실패 시 응답 메시지에 내부 상태 노출 없음
- [ ] 도메인 검증 실패는 사용자에게 의미 있는 메시지 유지

## Work Log

- 2026-04-27: ce-review 발견 (security-sentinel P2)