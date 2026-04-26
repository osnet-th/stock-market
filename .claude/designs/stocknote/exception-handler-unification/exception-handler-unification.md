# [stocknote] ExceptionHandler 응답 shape 통일 + Validation 핸들러 추가

> 분석: [exception-handler-unification](../../../analyzes/stocknote/exception-handler-unification/exception-handler-unification.md). plan task: Phase 10 P1 #12.

## 의도

`StockNoteExceptionHandler` 를 단일 응답 shape **`{error, message, timestamp}`** 로 통일 (GlobalExceptionHandler 와 일관). bean validation / JSON 파싱 / type mismatch 핸들러 3건 추가로 stocknote 컨트롤러의 모든 4xx 응답을 표준화.

## 변경 사항

### 단일 파일 변경: `StockNoteExceptionHandler.java`

```java
package com.thlee.stock.market.stockmarket.stocknote.presentation;

import com.thlee.stock.market.stockmarket.stocknote.application.exception.StockNoteLockedException;
import com.thlee.stock.market.stockmarket.stocknote.application.exception.StockNoteNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * stocknote 컨트롤러 전용 예외 매핑.
 *
 * <p>응답 shape 는 GlobalExceptionHandler 와 동일 — {@code {error, message, timestamp}}.
 * @Valid 검증 실패는 추가로 {@code fieldErrors} 를 포함한다.
 */
@Slf4j
@RestControllerAdvice(assignableTypes = {
        StockNoteController.class,
        StockNoteVerificationController.class,
        StockNoteAnalyticsController.class,
        StockNoteCustomTagController.class
})
public class StockNoteExceptionHandler {

    @ExceptionHandler(StockNoteNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(StockNoteNotFoundException e) {
        log.debug("stocknote not found: {}", e.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", "기록을 찾을 수 없습니다.");
    }

    @ExceptionHandler(StockNoteLockedException.class)
    public ResponseEntity<Map<String, Object>> handleLocked(StockNoteLockedException e) {
        return buildResponse(HttpStatus.CONFLICT, StockNoteLockedException.ERROR_CODE,
                "검증이 등록된 기록은 수정할 수 없습니다.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        String msg = e.getMessage() == null ? "잘못된 요청입니다." : e.getMessage();
        return buildResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", msg);
    }

    @ExceptionHandler(InsufficientAuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientAuthentication(InsufficientAuthenticationException e) {
        log.debug("stocknote insufficient authentication: {}", e.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
    }

    /** @Valid @RequestBody 필드 검증 실패. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage(),
                        (a, b) -> a
                ));
        Map<String, Object> body = baseBody("VALIDATION_FAILED", "요청 본문 검증에 실패했습니다.");
        body.put("fieldErrors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /** JSON 파싱 실패 / enum 값 매칭 실패 등. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleNotReadable(HttpMessageNotReadableException e) {
        log.debug("stocknote payload not readable: {}", e.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "요청 본문이 올바르지 않습니다.");
    }

    /** query/path 파라미터 타입 변환 실패 (?direction=foo, /snapshots/UNKNOWN/retry). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.debug("stocknote param type mismatch: name={}, value={}", e.getName(), e.getValue());
        return buildResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST",
                "잘못된 요청 파라미터 값: " + e.getName());
    }

    private static ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status).body(baseBody(error, message));
    }

    private static Map<String, Object> baseBody(String error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("message", message);
        body.put("timestamp", LocalDateTime.now().toString());
        return body;
    }
}
```

## 변경 동작

| 시나리오 | 변경 전 응답 | 변경 후 응답 |
|---|---|---|
| `GET /stock-notes/9999` (없는 ID) | `{error:"NOT_FOUND"}` | `{error:"NOT_FOUND", message:"기록을 찾을 수 없습니다.", timestamp:"..."}` |
| `PUT /stock-notes/{id}` (검증 존재) | `{error:"LOCKED_BY_VERIFICATION"}` | `{..., message:"검증이 등록된...", timestamp:"..."}` |
| `POST /stock-notes` (필드 누락) | **500 INTERNAL_ERROR** | **400 VALIDATION_FAILED** + `fieldErrors:{stockCode:"...", ...}` |
| `POST /stock-notes` (잘못된 enum JSON) | **500 INTERNAL_ERROR** | **400 BAD_REQUEST** "요청 본문이 올바르지 않습니다." |
| `GET /stock-notes?direction=long` | **500 INTERNAL_ERROR** | **400 BAD_REQUEST** "잘못된 요청 파라미터 값: direction" |
| 토큰 없음 | `{error:"UNAUTHORIZED"}` | `{..., message:"로그인이 필요합니다.", timestamp:"..."}` |

## 회귀 위험

| 위험 | 영향 | 완화 |
|---|---|---|
| 응답 shape 변경 — 클라이언트 파서 | 프론트는 stocknote.js 에서 e.message 만 사용 → message 키 유지로 호환 | 기존 error 키도 유지 |
| Map.of → LinkedHashMap | put 순서 보존 | n/a |
| MethodArgumentNotValidException 의 fieldErrors 직렬화 | Spring Jackson 자동 — 정상 | n/a |

## 작업 리스트

- [ ] `StockNoteExceptionHandler.java` 응답 shape 통일 + 핸들러 3건 추가
- [ ] `Map<String, String>` → `Map<String, Object>` 타입 변경
- [ ] private helper `buildResponse` / `baseBody` 추가
- [ ] 컴파일 확인
- [ ] plan checkbox 갱신 (P1 #12)

## 후속 task 자동 해결

- Task #37 (IllegalArgument msg null Map.of NPE) — 본 task 의 `e.getMessage() == null ? "..." : e.getMessage()` 가드로 해소
- Task #44 (enum query/path 미스 → 400) — 본 task 의 MethodArgumentTypeMismatchException 핸들러로 해소

## 승인 대기

태형님 승인 후 구현 진행.