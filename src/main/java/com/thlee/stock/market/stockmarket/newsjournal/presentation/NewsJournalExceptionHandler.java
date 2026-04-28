package com.thlee.stock.market.stockmarket.newsjournal.presentation;

import com.thlee.stock.market.stockmarket.newsjournal.application.exception.NewsEventNotFoundException;
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
 * newsjournal 컨트롤러 전용 예외 매핑.
 *
 * <p>응답 shape 는 stocknote / GlobalExceptionHandler 와 동일 — {@code {error, message, timestamp}}.
 * {@code @Valid} 검증 실패는 추가로 {@code fieldErrors} 를 포함한다.
 */
@Slf4j
@RestControllerAdvice(assignableTypes = NewsJournalController.class)
public class NewsJournalExceptionHandler {

    @ExceptionHandler(NewsEventNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NewsEventNotFoundException e) {
        log.debug("news event not found: {}", e.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", "사건을 찾을 수 없습니다.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        String msg = e.getMessage() == null ? "잘못된 요청입니다." : e.getMessage();
        return buildResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", msg);
    }

    @ExceptionHandler(InsufficientAuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientAuthentication(InsufficientAuthenticationException e) {
        log.debug("news journal insufficient authentication: {}", e.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
    }

    /** {@code @Valid @RequestBody} 필드 검증 실패. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage(),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
        Map<String, Object> body = baseBody("VALIDATION_FAILED", "요청 본문 검증에 실패했습니다.");
        body.put("fieldErrors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /** JSON 파싱 실패 / enum 값 매칭 실패 등. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleNotReadable(HttpMessageNotReadableException e) {
        log.debug("news journal payload not readable: {}", e.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "요청 본문이 올바르지 않습니다.");
    }

    /** query/path 파라미터 타입 변환 실패 (?category=foo, /events/abc). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.debug("news journal param type mismatch: name={}, value={}", e.getName(), e.getValue());
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