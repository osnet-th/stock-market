# [stocknote] @Valid 핸들러 미존재 + 응답 shape 4종 비일관

> ce-review 2026-04-25 P1 #12 (api-contract). plan task: Phase 10 P1.

## 현재 상태

### `StockNoteExceptionHandler` 4개 핸들러 — 응답 shape 모두 다름

| 핸들러 | 응답 |
|---|---|
| `StockNoteNotFoundException` → 404 | `{error: "NOT_FOUND"}` (message 없음) |
| `StockNoteLockedException` → 409 | `{error: "LOCKED_BY_VERIFICATION"}` (message 없음) |
| `IllegalArgumentException` → 400 | `{error: "BAD_REQUEST", message: <e.message>}` (timestamp 없음) |
| `InsufficientAuthenticationException` → 401 | `{error: "UNAUTHORIZED"}` (message 없음) |

### `GlobalExceptionHandler` 표준 — `{error, message, timestamp}`

stocknote 컨트롤러에서 미처리 예외가 `handleGeneral` 로 새면 4번째 shape 응답.

### 미처리 예외 → 글로벌 500 폭발

| 예외 | 발생 시점 | 현재 동작 |
|---|---|---|
| `MethodArgumentNotValidException` | `@Valid @RequestBody` 검증 실패 (CreateStockNoteRequest 등) | GlobalExceptionHandler.handleGeneral → **500 INTERNAL_ERROR** (의도는 400) |
| `HttpMessageNotReadableException` | JSON 파싱 실패 / 잘못된 enum 값 (UpsertVerificationRequest.judgmentResult="FOO") | 500 |
| `MethodArgumentTypeMismatchException` | query/path enum 미스 (`?direction=long`, `/snapshots/UNKNOWN/retry`) | 500 |
| `ConstraintViolationException` | `@PathVariable @Min(...)` 등 violation | 500 |

## 영향 범위

| 영역 | 영향 |
|---|---|
| 클라이언트 contract | 4종 응답 shape — 일반 파서 처리 불가능 |
| @Valid 동작 | 실패 시 사용자에게 "서버 오류" 표시 (의도는 400 + 필드별 에러) |
| enum mismatch | 사용자가 잘못된 query 값 보내면 500 → 디버그 어려움 |

## 해결

### 단일 옵션 — stocknote 응답 shape 를 GlobalExceptionHandler 표준으로 통일 + 누락 핸들러 추가

응답 shape: **`{error, message, timestamp}`** (GlobalExceptionHandler 와 동일).

추가 핸들러:
1. `MethodArgumentNotValidException` → 400 + 필드별 에러 메시지
2. `HttpMessageNotReadableException` → 400 + "요청 본문이 올바르지 않습니다"
3. `MethodArgumentTypeMismatchException` → 400 + "잘못된 요청 파라미터 값"

`ConstraintViolationException` 은 stocknote 컨트롤러에 `@Validated` 미사용이므로 불필요.

기존 4 핸들러도 동일 shape 로 변환:
- NotFound: `{error: "NOT_FOUND", message: "기록을 찾을 수 없습니다.", timestamp}`
- Locked: `{error: "LOCKED_BY_VERIFICATION", message: "검증이 등록된 기록은 수정할 수 없습니다.", timestamp}`
- IllegalArgument: `{error: "BAD_REQUEST", message: <e.message or 기본>, timestamp}`
- InsufficientAuthentication: `{error: "UNAUTHORIZED", message: "로그인이 필요합니다.", timestamp}`

### 헬퍼 메서드

GlobalExceptionHandler 의 `buildResponse` 와 동일 패턴을 stocknote 핸들러에서 재사용.

## 후속 task 와의 관계

| Task | 정합 |
|---|---|
| #37 P3 IllegalArgument msg null Map.of NPE 가드 | 본 task 의 buildResponse 가 message null 처리 통합 |
| #44 P3 enum query/path 미스 → 400 매핑 | 본 task 의 MethodArgumentTypeMismatchException 핸들러로 자동 해결 |
| #40 P3 IllegalArgument 메시지 노출 정제 | 별건 — 메시지 sanitize 정책은 추후 |

## 코드 위치

| 파일 | 변경 |
|---|---|
| `StockNoteExceptionHandler.java` | 응답 shape 통일 + 핸들러 3건 추가 + private helper |

## 설계 문서

[exception-handler-unification](../../../designs/stocknote/exception-handler-unification/exception-handler-unification.md)